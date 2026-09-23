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
import ar.uade.cine.service.usuarios.GestorClientes;

/**
 * Ciclo de vida de una reserva: se vende, se cancela o se usa en la puerta. Lo ocupado
 * lo define {@link Ocupacion}, así vender y dibujar el mapa usan la misma regla. Recibe
 * seis repositorios porque vender cruza seis agregados relacionados por id; lo que tiene
 * regla propia (precio, ocupación, alta de cliente) vive en su clase.
 */
@Service
@Transactional
public class GestorReservas {

    private static final Logger LOG = LoggerFactory.getLogger(GestorReservas.class);

    private final ReservaRepository reservaRepository;
    private final FuncionRepository funcionRepository;
    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final ClienteRepository clienteRepository;
    private final GestorClientes clientes;
    private final PeliculaRepository peliculaRepository;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio;
    private final Ocupacion ocupacion;
    private final Reloj reloj;

    public GestorReservas(ReservaRepository reservaRepository, FuncionRepository funcionRepository, SalaRepository salaRepository,
                          AsientoRepository asientoRepository, ClienteRepository clienteRepository, GestorClientes clientes,
                          PeliculaRepository peliculaRepository,
                          GeneradorTicket generadorTicket, CalculadoraPrecio calculadoraPrecio,
                          Ocupacion ocupacion, Reloj reloj) {
        this.reservaRepository = reservaRepository;
        this.funcionRepository = funcionRepository;
        this.salaRepository = salaRepository;
        this.asientoRepository = asientoRepository;
        this.clienteRepository = clienteRepository;
        this.clientes = clientes;
        this.peliculaRepository = peliculaRepository;
        this.generadorTicket = generadorTicket;
        this.calculadoraPrecio = calculadoraPrecio;
        this.ocupacion = ocupacion;
        this.reloj = reloj;
    }

    /** Sin sesión: la boletería, donde elegir y confirmar son un solo acto. */
    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas) {
        return reservar(funcionId, clienteId, butacas, null);
    }

    /**
     * Compra sin registro: el email identifica o da de alta al cliente. Va acá para que el
     * alta comparta la transacción y una reserva rechazada no deje al cliente creado.
     */
    public Reserva reservar(int funcionId, String nombre, String email, Map<String, TipoTarifa> butacas,
                            String sesion) {
        Cliente cliente = clientes.identificar(nombre, email);
        return reservar(funcionId, cliente.getId(), butacas, sesion);
    }

    /**
     * Butacas como mapa código → tarifa: la tarifa es por persona y el mapa impide repetir.
     *
     * @param sesion para que su propio bloqueo no le rechace la reserva
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

        List<Entrada> entradas = armarEntradas(funcion, sala, butacas, sesion);
        Reserva reserva = guardarCompitiendoPorLasButacas(
                new Reserva(funcionId, clienteId, entradas, reloj.ahora()));
        // Desde acá la butaca la retiene la reserva, no el bloqueo.
        if (sesion != null) {
            ocupacion.liberar(funcionId, sesion);
        }
        LOG.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcionId,
                detalleDe(entradas), reserva.getTotal());

        emitirTicket(reserva, funcion, sala, cliente);
        return reserva;
    }

    /** Asientos y ocupados se leen una vez por pedido, no por butaca. */
    private List<Entrada> armarEntradas(Funcion funcion, Sala sala, Map<String, TipoTarifa> butacas,
                                        String sesion) {
        List<Asiento> deLaSala = asientoRepository.findBySalaIdOrderByFilaAscNumeroAsc(funcion.getSalaId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcion.getId(), sesion);

        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            TipoTarifa tarifa = pedido.getValue() == null ? TipoTarifa.GENERAL : pedido.getValue();
            Asiento asiento = butacaVendible(deLaSala, pedido.getKey(), ocupados);
            entradas.add(new Entrada(asiento, tarifa,
                    calculadoraPrecio.precioDe(funcion, sala, asiento, tarifa)));
        }
        return entradas;
    }

    private static Asiento butacaVendible(List<Asiento> deLaSala, String codigo, Set<Integer> ocupados) {
        // Buscar entre los de esta sala garantiza que sea de la sala de la función; la base no lo valida.
        Asiento asiento = Asiento.conCodigo(deLaSala, codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La butaca " + Asiento.normalizarCodigo(codigo) + " no existe en esa sala"));
        // R9
        if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " está fuera de servicio");
        }
        // R4
        if (ocupados.contains(asiento.getId())) {
            throw new IllegalArgumentException("La butaca " + asiento.getCodigo() + " ya está ocupada");
        }
        return asiento;
    }

    private void emitirTicket(Reserva reserva, Funcion funcion, Sala sala, Cliente cliente) {
        Pelicula pelicula = peliculaRepository.findById(funcion.getPeliculaId()).orElseThrow();
        generadorTicket.emitir(reserva, funcion, pelicula, sala, cliente);
    }

    /** R6 y R13, en {@link Reserva#cancelar()}. */
    public void cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        reserva.cancelar();
        reservaRepository.save(reserva);
        LOG.info("reserva {} CANCELADA · {} butacas vuelven a la venta",
                reservaId, reserva.getCantidadEntradas());
    }

    /**
     * R18. Por código y no por id: es la credencial del QR; con el id se entraría probando números.
     *
     * @return la reserva, para que el acomodador vea la tarifa de cada butaca
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
     * En memoria y no con {@code WHERE}: el texto cruza cuatro tablas y un JOIN partiría el
     * criterio entre el gestor y JPQL. Aguanta decenas de miles de reservas.
     */
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return listar();
        }
        // Catálogos indexados una vez, para no consultar por fila.
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

    /** Por lo que el cliente tiene a mano: nombre, mail, película, código o butaca. */
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

    /** Para el log: {@code C4(JUBILADO) C5(GENERAL)}. */
    private static String detalleDe(List<Entrada> entradas) {
        return entradas.stream()
                .map(e -> e.codigoAsiento() + "(" + e.tarifa() + ")")
                .collect(Collectors.joining(" "));
    }

    /**
     * Entre validar y escribir otro pudo tomar la butaca: lo corta el {@code UNIQUE
     * (funcion_id, asiento_id)}, y {@code saveAndFlush} hace que salte acá como 409 y no
     * como 500 en el commit.
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
