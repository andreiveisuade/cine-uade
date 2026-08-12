package ar.uade.cine.servicio;

import java.util.List;
import java.util.Optional;

import ar.uade.cine.interfaces.Cliente;
import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.interfaces.Funcion;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.GeneradorTicket;
import ar.uade.cine.interfaces.Pelicula;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.interfaces.Sala;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.ReservaImpl;

/**
 * Reservar, pagar y cancelar. Necesita los otros DAOs porque el cupo depende de la
 * capacidad de la sala de la función, y el ticket lleva los datos de todos.
 */
public class GestorReservas {

    private final ReservaDAO reservaDAO;
    private final FuncionDAO funcionDAO;
    private final SalaDAO salaDAO;
    private final ClienteDAO clienteDAO;
    private final PeliculaDAO peliculaDAO;
    private final GeneradorTicket generadorTicket;

    public GestorReservas(ReservaDAO reservaDAO, FuncionDAO funcionDAO, SalaDAO salaDAO,
                          ClienteDAO clienteDAO, PeliculaDAO peliculaDAO, GeneradorTicket generadorTicket) {
        this.reservaDAO = reservaDAO;
        this.funcionDAO = funcionDAO;
        this.salaDAO = salaDAO;
        this.clienteDAO = clienteDAO;
        this.peliculaDAO = peliculaDAO;
        this.generadorTicket = generadorTicket;
    }

    /** Crea la reserva y emite el ticket. Devuelve la reserva ya con su id. */
    public Reserva reservar(int funcionId, int clienteId, int cantidad) {
        Funcion funcion = funcionDAO.buscarPorId(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        Cliente cliente = clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de entradas debe ser mayor a cero");
        }

        int libres = lugaresLibres(funcionId);
        if (cantidad > libres) {
            throw new IllegalArgumentException("Solo quedan " + libres + " lugares en esa función");
        }

        Reserva reserva = new ReservaImpl(funcionId, clienteId, cantidad);
        reservaDAO.guardar(reserva);

        Sala sala = salaDAO.buscarPorId(funcion.getSalaId()).orElseThrow();
        Pelicula pelicula = peliculaDAO.buscarPorId(funcion.getPeliculaId()).orElseThrow();
        generadorTicket.emitir(reserva, funcion, pelicula, sala, cliente);

        return reserva;
    }

    public void pagar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        if (reserva.getEstado() != EstadoReserva.RESERVADA) {
            throw new IllegalArgumentException("La reserva está " + reserva.getEstado() + ", no se puede pagar");
        }
        reserva.setEstado(EstadoReserva.PAGADA);
        reservaDAO.actualizar(reserva);
    }

    public void cancelar(int reservaId) {
        Reserva reserva = buscarOFallar(reservaId);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("La reserva ya está cancelada");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaDAO.actualizar(reserva);
    }

    /** Capacidad de la sala menos lo reservado. Las canceladas no ocupan lugar. */
    public int lugaresLibres(int funcionId) {
        Funcion funcion = funcionDAO.buscarPorId(funcionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la función " + funcionId));
        Sala sala = salaDAO.buscarPorId(funcion.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + funcion.getSalaId()));

        int ocupados = reservaDAO.listarPorFuncion(funcionId).stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .mapToInt(Reserva::getCantidadEntradas)
                .sum();
        return sala.getCapacidad() - ocupados;
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

    private Reserva buscarOFallar(int id) {
        return reservaDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva " + id));
    }
}
