package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservar y comprar son el mismo registro en distinto estado: nace RESERVADA
 * y pasa a PAGADA cuando el cliente abona.
 */
public class ReservaImpl implements Reserva {

    private int id;
    private final int funcionId;
    private final int clienteId;
    private final LocalDateTime creadaEn;
    private final List<Entrada> entradas = new ArrayList<>();
    private EstadoReserva estado;

    /** Reserva nueva: arranca RESERVADA, todavía no tiene id. */
    public ReservaImpl(int funcionId, int clienteId, List<Entrada> entradas, LocalDateTime creadaEn) {
        this.funcionId = funcionId;
        this.clienteId = clienteId;
        this.creadaEn = creadaEn;
        this.entradas.addAll(entradas);
        this.estado = EstadoReserva.RESERVADA;
    }

    /** Reserva que viene de la base, con el estado que tenga guardado. */
    public ReservaImpl(int id, int funcionId, int clienteId, List<Entrada> entradas,
                       EstadoReserva estado, LocalDateTime creadaEn) {
        this(funcionId, clienteId, entradas, creadaEn);
        this.id = id;
        this.estado = estado;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getFuncionId() {
        return funcionId;
    }

    @Override
    public int getClienteId() {
        return clienteId;
    }

    @Override
    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    @Override
    public List<Entrada> getEntradas() {
        return new ArrayList<>(entradas);
    }

    @Override
    public void agregarEntrada(Entrada entrada) {
        entradas.add(entrada);
    }

    @Override
    public int getCantidadEntradas() {
        return entradas.size();
    }

    @Override
    public double getTotal() {
        return entradas.stream().mapToDouble(Entrada::precio).sum();
    }

    @Override
    public EstadoReserva getEstado() {
        return estado;
    }

    @Override
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "[" + id + "] función " + funcionId + " - cliente " + clienteId
                + " - butacas " + entradas + " - " + estado;
    }
}
