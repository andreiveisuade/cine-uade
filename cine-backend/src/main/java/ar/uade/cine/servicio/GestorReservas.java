package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.salas.Asiento;
import ar.uade.cine.dominio.salas.EstadoAsiento;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.EstadoReserva;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.ReservaImpl;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.comprobantes.GeneradorTicket;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * El ciclo de vida de una reserva: nace al vender, se cancela, o se usa en la puerta.
 *
 * <p>Qué butacas están tomadas <em>no</em> se resuelve acá: eso es sobre la función, no
 * sobre una reserva, y vive en {@link Ocupacion}. Este gestor se la pregunta al vender,
 * igual que la hacen la consola y la API para dibujar el mapa, así que los tres validan
 * contra la misma definición de "ocupado".
 */
public class GestorReservas {

    /**
     * La bitácora del negocio. Registra lo que <strong>pasó</strong> —una reserva, un
     * ingreso, una expiración—, no por dónde pasó el código: nadie necesita saber que se
     * entró a un método, y un log de trazas se vuelve ilegible justo el día que hay que
     * usarlo.
     *
     * <p>Va en el gestor y no en la capa HTTP porque es la capa que decide. Vender por la
     * API y vender por el menú de consola son la misma operación del cine y tienen que
     * dejar la misma línea. SLF4J es una fachada y llega hasta acá; el paquete
     * {@code dominio} sigue sin depender de nada externo.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GestorReservas.class);

    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final SalaDAO salaDAO;
    private final AsientoDAO asientoDAO;
    private final ClienteDAO clienteDAO;
    private final PeliculaDAO peliculaDAO;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio;
    private final Ocupacion ocupacion;

    /**
     * La calculadora entra por el constructor como todo lo demás. Creándola adentro, el
     * precio quedaba fijado por dentro de una regla de negocio y esta clase decidía sola
     * cómo se cobra; recibida, es una pieza que se puede cambiar sin tocar el gestor.
     */
    public GestorReservas(ReservaDAO reservaDAO, FuncionDAO funcionDAO, SalaDAO salaDAO,
                          AsientoDAO asientoDAO, ClienteDAO clienteDAO, PeliculaDAO peliculaDAO,
                          GeneradorTicket generadorTicket, CalculadoraPrecio calculadoraPrecio,
                          Ocupacion ocupacion) {
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.salaDAO = salaDAO;
        this.asientoDAO = asientoDAO;
        this.clienteDAO = clienteDAO;
        this.peliculaDAO = peliculaDAO;
        this.generadorTicket = generadorTicket;
        this.calculadoraPrecio = calculadoraPrecio;
        this.ocupacion = ocupacion;
    }

    /**
     * Crea la reserva con las butacas elegidas y emite el ticket.
     *
     * <p>Las butacas vienen como código a tarifa porque la tarifa es por persona: en una
     * reserva de cuatro puede haber dos generales, un menor y un jubilado. Que sea un mapa
     * y no una lista hace además que una butaca repetida sea imposible por construcción,
     * en vez de un error que hay que validar.
     *
     * @param butacas código de butaca a tarifa de quien la ocupa
     */
    public Reserva reservar(int funcionId, int clienteId, Map<String, TipoTarifa> butacas) {
        Funcion funcion = buscarFuncion(funcionId);
        // R19: una función que ya arrancó no se vende. Va antes que todo lo demás porque
        // ninguna de las otras validaciones tiene sentido si la película ya está dada:
        // no importa si la butaca está libre cuando la función empezó hace media hora.
        if (funcion.yaEmpezo(LocalDateTime.now())) {
            throw new IllegalArgumentException("La función ya empezó: no se pueden reservar butacas");
        }
        Cliente cliente = clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (butacas == null || butacas.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos una butaca");
        }

        Sala sala = salaDAO.buscarPorId(funcion.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + funcion.getSalaId()));
        List<Asiento> deLaSala = asientoDAO.listarPorSala(funcion.getSalaId());
        Set<Integer> ocupados = ocupacion.asientosOcupados(funcionId);

        List<Entrada> entradas = new ArrayList<>();
        for (Map.Entry<String, TipoTarifa> pedido : butacas.entrySet()) {
            String buscado = pedido.getKey().trim().toUpperCase();
            TipoTarifa tarifa = pedido.getValue() == null ? TipoTarifa.GENERAL : pedido.getValue();
            // Buscar el asiento entre los de esta sala es lo que garantiza que la butaca
            // pertenezca a la sala de la función: la base no lo puede validar sola.
            Asiento asiento = deLaSala.stream()
                    .filter(a -> a.getCodigo().equals(buscado))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La butaca " + buscado + " no existe en esa sala"));
            // R9: una butaca fuera de servicio no se vende en ninguna función.
            if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
                throw new IllegalArgumentException("La butaca " + buscado + " está fuera de servicio");
            }
            // R4: no se puede reservar una butaca ya tomada en esa función.
            if (ocupados.contains(asiento.getId())) {
                throw new IllegalArgumentException("La butaca " + buscado + " ya está ocupada");
            }
            entradas.add(new Entrada(asiento.getId(), asiento.getCodigo(), tarifa,
                    calculadoraPrecio.precioDe(funcion, sala, asiento, tarifa)));
        }

        Reserva reserva = new ReservaImpl(funcionId, clienteId, entradas, LocalDateTime.now());
        reservaDAO.guardar(reserva);
        // El detalle por butaca y no solo el total: cuando haya que explicar por qué esta
        // reserva salió lo que salió, la tarifa de cada una es la mitad de la respuesta.
        LOG.info("reserva {} creada · funcion {} · {} · total {}", reserva.getId(), funcionId,
                detalleDe(entradas), reserva.getTotal());

        Pelicula pelicula = peliculaDAO.buscarPorId(funcion.getPeliculaId()).orElseThrow();
        generadorTicket.emitir(reserva, funcion, pelicula, sala, cliente);

        return reserva;
    }

    /**
     * R6: las butacas vuelven a estar disponibles. R13: una reserva ya cobrada no se
     * cancela sin más, porque el pago seguiría contando en el arqueo del día; devolver
     * la plata es un circuito de boletería que este sistema todavía no modela.
     */
    public void cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado()
                    + ", solo se puede cancelar una reserva sin cobrar");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaDAO.actualizar(reserva);
        LOG.info("reserva {} CANCELADA · {} butacas vuelven a la venta",
                reservaId, reserva.getCantidadEntradas());
    }

    /**
     * R18: valida la entrada en la puerta. Solo pasa una reserva pagada, y una sola vez.
     *
     * <p>Busca por código y no por id porque es lo que trae el QR, y es además la única
     * credencial del cliente: pedir el id dejaría entrar probando números.
     *
     * @return la reserva ya marcada como ingresada, para que el acomodador vea las
     *         butacas y qué tarifa declaró cada una
     */
    public Reserva registrarIngreso(String codigo) {
        Reserva reserva = reservaDAO.buscarPorCodigo(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna reserva con ese código"));
        if (reserva.getEstado() != EstadoReserva.PAGADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado()
                    + ": solo se ingresa con una reserva pagada");
        }
        if (reserva.getIngresadaEn() != null) {
            throw new IllegalArgumentException("Esa entrada ya se usó el "
                    + reserva.getIngresadaEn());
        }
        reserva.setIngresadaEn(LocalDateTime.now());
        reservaDAO.actualizar(reserva);
        LOG.info("ingreso reserva {} · codigo {} · {} personas",
                reserva.getId(), reserva.getCodigo(), reserva.getCantidadEntradas());
        return reserva;
    }

    public List<Reserva> listarPorCliente(int clienteId) {
        return reservaDAO.listarPorCliente(clienteId);
    }

    public List<Reserva> listar() {
        return reservaDAO.listar();
    }

    /**
     * Las reservas que cumplen los criterios.
     *
     * <p>El filtrado se hace acá y no en la pantalla porque los criterios son del
     * negocio: «pendientes de cobro» tiene una definición y tiene que ser la misma para
     * la web, para la consola y para cualquier cosa que venga después.
     *
     * <p>Se resuelve sobre la lista en memoria y no con un {@code WHERE} en SQL. Es una
     * decisión consciente y tiene un techo: con decenas de miles de reservas habría que
     * bajarlo al DAO. Hoy la búsqueda cruza datos de cuatro tablas —el nombre del
     * cliente, el título de la película, los códigos de butaca— y escribir ese JOIN a
     * mano dejaría el criterio partido entre el gestor y una cadena de SQL, que es
     * justamente lo que el patrón DAO viene a evitar.
     */
    public List<Reserva> buscar(CriteriosReserva criterios) {
        if (criterios == null || criterios.sinFiltros()) {
            return listar();
        }
        return reservaDAO.listar().stream()
                .filter(r -> criterios.estado() == null || r.getEstado() == criterios.estado())
                .filter(r -> criterios.dia() == null || esDelDia(r, criterios.dia()))
                .filter(r -> coincideElTexto(r, criterios.textoNormalizado()))
                .toList();
    }

    private boolean esDelDia(Reserva reserva, LocalDate dia) {
        return funcionDAO.buscarPorId(reserva.getFuncionId())
                .map(f -> f.getInicio().toLocalDate().equals(dia))
                .orElse(false);
    }

    /**
     * Busca por lo que la persona tiene a mano cuando pregunta: su nombre, su mail, la
     * película que vino a ver, el código del ticket o la butaca que dice tener.
     */
    private boolean coincideElTexto(Reserva reserva, String texto) {
        if (texto.isEmpty()) {
            return true;
        }
        if (contiene(reserva.getCodigo(), texto)) {
            return true;
        }
        if (reserva.getEntradas().stream().anyMatch(e -> contiene(e.codigoAsiento(), texto))) {
            return true;
        }
        boolean porCliente = clienteDAO.buscarPorId(reserva.getClienteId())
                .map(c -> contiene(c.getNombre(), texto) || contiene(c.getEmail(), texto))
                .orElse(false);
        if (porCliente) {
            return true;
        }
        return funcionDAO.buscarPorId(reserva.getFuncionId())
                .flatMap(f -> peliculaDAO.buscarPorId(f.getPeliculaId()))
                .map(p -> contiene(p.getTitulo(), texto))
                .orElse(false);
    }

    private static boolean contiene(String campo, String texto) {
        return campo != null && campo.toLowerCase().contains(texto);
    }

    public Optional<Reserva> buscar(int id) {
        return reservaDAO.buscarPorId(id);
    }

    /** Las butacas con su tarifa, para la bitácora: {@code C4(JUBILADO) C5(GENERAL)}. */
    private static String detalleDe(List<Entrada> entradas) {
        return entradas.stream()
                .map(e -> e.codigoAsiento() + "(" + e.tarifa() + ")")
                .collect(Collectors.joining(" "));
    }

    private Funcion buscarFuncion(int funcionId) {
        return funcionDAO.buscarPorId(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
    }

    private Reserva buscarOFallar(int id) {
        return reservaDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + id));
    }
}
