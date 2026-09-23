package ar.uade.cine.service.ventas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.infrastructure.comprobantes.GeneradorTicket;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.EstadoAsiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.AsientoRepository;
import ar.uade.cine.repository.ClienteRepository;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.repository.SalaRepository;

/**
 * El ciclo de vida de una reserva: nace al vender, se cancela, o se usa en la puerta.
 * Qué butacas están tomadas no se resuelve acá sino en {@link Ocupacion}: es un hecho
 * sobre la función, y así vender y dibujar el mapa usan la misma definición.
 */
@Service
@Transactional
public class GestorReservas {

    /** La bitácora del negocio: qué pasó, no por dónde pasó el código. */
    private static final Logger LOG = LoggerFactory.getLogger(GestorReservas.class);

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final ClienteRepository clienteRepository;
    private final PeliculaRepository peliculaRepository;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio;
    private final Ocupacion ocupacion;
    private final Reloj reloj;

    public GestorReservas(ReservaRepository reservaRepository, FuncionRepository funcionRepository, SalaRepository salaRepository,
                          AsientoRepository asientoRepository, ClienteRepository clienteRepository, PeliculaRepository peliculaRepository,
                          GeneradorTicket generadorTicket, CalculadoraPrecio calculadoraPrecio,
                          Ocupacion ocupacion, Reloj reloj) {
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.salaRepository = salaRepository;
        this.asientoRepository = asientoRepository;
        this.clienteRepository = clienteRepository;
        this.peliculaRepository = peliculaRepository;
        this.generadorTicket = generadorTicket;
        this.calculadoraPrecio = calculadoraPrecio;
        this.ocupacion = ocupacion;
        this.reloj = reloj;
    }

    /**
     * Reservar sin sesión previa: la boletería, donde elegir y confirmar son un solo acto
     * y no hay etapa de "mirando el mapa" que proteger. Sobrecarga y no {@code null} en la
     * llamada para que el caso quede escrito y no parezca un olvido.
     */
    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas) {
        return reservar(funcionId, clienteId, butacas, null);
    }

    /**
     * Crea la reserva con las butacas elegidas y emite el ticket. Las butacas vienen como
     * mapa código → tarifa porque la tarifa es por persona, y el mapa hace imposible una
     * butaca repetida.
     *
     * @param sesion quién viene eligiendo, para que su propio bloqueo no le rechace la
     *               reserva. Sin sesión, cualquier butaca bloqueada está tomada
     */
    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Funcion funcion = buscarFuncion(funcionId);
        // R19: una función que ya arrancó no se vende; va primero porque anula las demás.
        if (funcion.yaEmpezo(reloj.ahora())) {
            throw new IllegalArgumentException("La función ya empezó: no se pueden reservar butacas");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (butacas == null || butacas.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos una butaca");
        }

        Sala sala = salaRepository.findById(funcion.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + funcion.getSalaId()));
        List<Asiento> deLaSala = asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(funcion.getSalaId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcionId, sesion);

        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            TipoTarifa tarifa = pedido.getValue() == null ? TipoTarifa.GENERAL : pedido.getValue();
            // Buscar entre los asientos de esta sala es lo que garantiza que la butaca
            // pertenezca a la sala de la función: la base no lo puede validar sola.
            Asiento asiento = Asiento.conCodigo(deLaSala, pedido.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La butaca " + Asiento.normalizarCodigo(pedido.getKey()) + " no existe en esa sala"));
            // R9: una butaca fuera de servicio no se vende en ninguna función.
            if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
                throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " está fuera de servicio");
            }
            // R4: no se puede reservar una butaca ya tomada en esa función.
            if (ocupados.contains(asiento.getId())) {
                throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " ya está ocupada");
            }
            entradas.add(new Entrada(asiento, tarifa,
                    calculadoraPrecio.precioDe(funcion, sala, asiento, tarifa)));
        }

        Reserva reserva = guardarCompitiendoPorLasButacas(
                new Reserva(funcionId, clienteId, entradas, reloj.ahora()));
        // Guardada la reserva, la butaca la retiene ella: el bloqueo cumplió su etapa.
        if (sesion != null) {
            ocupacion.liberar(funcionId, sesion);
        }
        LOG.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcionId,
                detalleDe(entradas), reserva.getTotal());

        Pelicula pelicula = peliculaRepository.findById(funcion.getPeliculaId()).orElseThrow();
        generadorTicket.emitir(reserva, funcion, pelicula, sala, cliente);

        return reserva;
    }

    /** R6 y R13: las reglas están en {@link Reserva#cancelar()}; acá se persiste y se registra. */
    public void cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        reserva.cancelar();
        reservaRepository.save(reserva);
        LOG.info("reserva {} CANCELADA · {} butacas vuelven a la venta",
                reservaId, reserva.getCantidadEntradas());
    }

    /**
     * R18: la entrada en la puerta. Busca por código y no por id porque es lo que trae el
     * QR y la única credencial del cliente: con el id se entraría probando números.
     *
     * @return la reserva ya ingresada, para que el acomodador vea qué tarifa declaró cada butaca
     */
    public Reserva registrarIngreso(String codigo) {
        Reserva reserva = reservaRepository.findByCodigo(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna reserva con ese código"));
        reserva.registrarIngreso(reloj.ahora());
        reservaRepository.save(reserva);
        LOG.info("ingreso reserva {} · codigo {} · {} personas",
                reserva.getId(), reserva.getCodigo(), reserva.getCantidadEntradas());
        return reserva;
    }

    public List<Reserva> listarPorCliente(int clienteId) {
        return reservaRepository.findByClienteIdOrderByCreadaEnDesc(clienteId);
    }

    public List<Reserva> listar() {
        return reservaRepository.findAll();
    }

    /**
     * Las reservas que cumplen los criterios. Se filtra acá y no en la pantalla porque
     * «pendientes de cobro» es una definición del negocio. Se resuelve en memoria y no
     * con un {@code WHERE}: el texto cruza cuatro tablas y el JOIN a mano partiría el
     * criterio entre el gestor y una cadena JPQL. Techo: decenas de miles de reservas.
     */
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return listar();
        }
        // Los catálogos se traen una vez, indexados: pedir la función, el cliente y la
        // película de cada reserva eran hasta tres consultas por fila.
        Map<Integer, Funcion> funciones = porId(funcionRepository.findAll(), Funcion::getId);
        Map<Integer, Pelicula> peliculas = porId(peliculaRepository.findAll(), Pelicula::getId);
        Map<Integer, Cliente> clientes = porId(clienteRepository.findAll(), Cliente::getId);
        String texto = criterios.textoNormalizado();
        return reservaRepository.findAll().stream()
                .filter(r -> criterios.estado() == null || r.getEstado() == criterios.estado())
                .filter(r -> criterios.dia() == null
                        || esDelDia(funciones.get(r.getFuncionId()), criterios.dia()))
                .filter(r -> coincideElTexto(r, texto, clientes.get(r.getClienteId()),
                        peliculaDe(funciones.get(r.getFuncionId()), peliculas)))
                .toList();
    }

    private static <T> Map<Integer, T> porId(List<T> elementos, Function<T, Integer> id) {
        return elementos.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static boolean esDelDia(Funcion funcion, LocalDate dia) {
        return funcion != null && funcion.getInicio().toLocalDate().equals(dia);
    }

    private static Pelicula peliculaDe(Funcion funcion, Map<Integer, Pelicula> peliculas) {
        return funcion == null ? null : peliculas.get(funcion.getPeliculaId());
    }

    /**
     * Busca por lo que la persona tiene a mano cuando pregunta: su nombre, su mail, la
     * película que vino a ver, el código del ticket o la butaca que dice tener.
     */
    private static boolean coincideElTexto(Reserva reserva, String texto, Cliente cliente,
                                           Pelicula pelicula) {
        if (texto.isEmpty() || contiene(reserva.getCodigo(), texto)) {
            return true;
        }
        if (reserva.getEntradas().stream().anyMatch(e -> contiene(e.codigoAsiento(), texto))) {
            return true;
        }
        if (cliente != null && (contiene(cliente.getNombre(), texto) || contiene(cliente.getEmail(), texto))) {
            return true;
        }
        return pelicula != null && contiene(pelicula.getTitulo(), texto);
    }

    private static boolean contiene(String campo, String texto) {
        return campo != null && campo.toLowerCase().contains(texto);
    }

    public Optional<Reserva> buscar(int id) {
        return reservaRepository.findById(id);
    }

    /** Las butacas con su tarifa, para la bitácora: {@code C4(JUBILADO) C5(GENERAL)}. */
    private static String detalleDe(List<Entrada> entradas) {
        return entradas.stream()
                .map(e -> e.codigoAsiento() + "(" + e.tarifa() + ")")
                .collect(Collectors.joining(" "));
    }

    /**
     * Guarda sabiendo que puede perder la carrera por la última butaca: entre validar y
     * escribir, otro pudo confirmar la misma. La cierra el {@code UNIQUE (funcion_id,
     * asiento_id)} de la base, y {@code saveAndFlush} hace que la violación salte acá y
     * salga como 409, no como un 500 en el commit.
     */
    private Reserva guardarCompitiendoPorLasButacas(Reserva reserva) {
        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            throw new ButacaOcupadaException(
                    "Alguien tomó una de esas butacas mientras confirmabas la reserva", e);
        }
    }

    private Funcion buscarFuncion(int funcionId) {
        return funcionRepository.findById(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
    }

    private Reserva buscarOFallar(int id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + id));
    }
}
