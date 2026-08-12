package ar.uade.cine.modelo;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Entrada;
import ar.uade.cine.interfaces.Reserva;

/**
 * Reservar y comprar son el mismo registro en distinto estado: nace RESERVADA
 * y pasa a PAGADA cuando el cliente abona.
 */
public class ReservaImpl implements Reserva {

    private int id;
    private int funcionId;
    private int clienteId;
    private final List<Entrada> entradas = new ArrayList<>();
    private EstadoReserva estado;

    public ReservaImpl(int funcionId, int clienteId, List<Entrada> entradas) {
        this.funcionId = funcionId;
        this.clienteId = clienteId;
        this.entradas.addAll(entradas);
        this.estado = EstadoReserva.RESERVADA;
    }

    public ReservaImpl(int id, int funcionId, int clienteId, List<Entrada> entradas, EstadoReserva estado) {
        this(funcionId, clienteId, entradas);
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
