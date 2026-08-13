package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.GeneradorTicket;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * Reservar y cancelar. Un asiento no está "ocupado" en sí mismo: lo está para una
 * función, si alguna reserva no cancelada de esa función lo tomó.
 */
public class GestorReservas {

    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final SalaDAO salaDAO;
    private final AsientoDAO asientoDAO;
    private final ClienteDAO clienteDAO;
    private final PeliculaDAO peliculaDAO;
    private final GeneradorTicket generadorTicket;
    private final CalculadoraPrecio calculadoraPrecio = new CalculadoraPrecio();

    public GestorReservas(ReservaDAO reservaDAO, FuncionDAO funcionDAO, SalaDAO salaDAO,
                          AsientoDAO asientoDAO, ClienteDAO clienteDAO, PeliculaDAO peliculaDAO,
                          GeneradorTicket generadorTicket) {
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.salaDAO = salaDAO;
        this.asientoDAO = asientoDAO;
        this.clienteDAO = clienteDAO;
        this.peliculaDAO = peliculaDAO;
        this.generadorTicket = generadorTicket;
    }

    /** Butacas de la sala que todavía nadie tomó para esa función. */
    public List<Asiento> asientosLibres(int funcionId) {
        Funcion funcion = buscarFuncion(funcionId);
        Set<Integer> ocupados = asientosOcupados(funcionId);
        return asientoDAO.listarPorSala(funcion.getSalaId()).stream()
                .filter(a -> a.getEstado() != EstadoAsiento.FUERA_DE_SERVICIO)
                .filter(a -> !ocupados.contains(a.getId()))
                .toList();
    }

    /** Crea la reserva con las butacas elegidas y emite el ticket. */
    public Reserva reservar(int funcionId, int clienteId, List<String> codigosAsiento) {
        Funcion funcion = buscarFuncion(funcionId);
        Cliente cliente = clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (codigosAsiento == null || codigosAsiento.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos una butaca");
        }

        Sala sala = salaDAO.buscarPorId(funcion.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + funcion.getSalaId()));
        List<Asiento> deLaSala = asientoDAO.listarPorSala(funcion.getSalaId());
        Set<Integer> ocupados = asientosOcupados(funcionId);

        List<Entrada> entradas = new ArrayList<>();
        List<String> yaElegidos = new ArrayList<>();
        for (String codigo : codigosAsiento) {
            String buscado = codigo.trim().toUpperCase();
            if (yaElegidos.contains(buscado)) {
                throw new IllegalArgumentException("La butaca " + buscado + " está repetida");
            }
            // Buscar el asiento entre los de esta sala es lo que garantiza que la butaca
            // pertenezca a la sala de la función: la base no lo puede validar sola.
            Asiento asiento = deLaSala.stream()
                    .filter(a -> a.getCodigo().equals(buscado))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La butaca " + buscado + " no existe en esa sala"));
            if (asiento.getEstado() == EstadoAsiento.FUERA_DE_SERVICIO) {
                throw new IllegalArgumentException("La butaca " + buscado + " está fuera de servicio");
            }
            if (ocupados.contains(asiento.getId())) {
                throw new IllegalArgumentException("La butaca " + buscado + " ya está ocupada");
            }
            yaElegidos.add(buscado);
            entradas.add(new Entrada(asiento.getId(), asiento.getCodigo(),
                    calculadoraPrecio.precioDe(funcion, sala, asiento)));
        }

        Reserva reserva = new ReservaImpl(funcionId, clienteId, entradas, LocalDateTime.now());
        reservaDAO.guardar(reserva);

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
    }

    public int lugaresLibres(int funcionId) {
        return asientosLibres(funcionId).size();
    }

    public List<Reserva> listarPorCliente(int clienteId) {
        return reservaDAO.listarPorCliente(clienteId);
    }

    public List<Reserva> listar() {
        return reservaDAO.listar();
    }

    public Optional<Reserva> buscar(int id) {
        return reservaDAO.buscarPorId(id);
    }

    /**
     * Ids de las butacas tomadas en esa función. Las reservas canceladas liberan las
     * suyas (R6). Es público porque es la regla que define "ocupado", y quien dibuje el
     * mapa de la sala —la consola o una API— tiene que preguntarla acá y no rehacerla.
     */
    public Set<Integer> asientosOcupados(int funcionId) {
        return reservaDAO.listarPorFuncion(funcionId).stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .flatMap(r -> r.getEntradas().stream())
                .map(Entrada::asientoId)
                .collect(Collectors.toSet());
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
