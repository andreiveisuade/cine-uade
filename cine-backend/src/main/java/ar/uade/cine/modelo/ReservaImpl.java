package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Reserva;

/**
 * Reservar y comprar son el mismo registro en distinto estado: nace RESERVADA
 * y pasa a PAGADA cuando el cliente abona.
 */
public class ReservaImpl implements Reserva {

    private int id;
    private int funcionId;
    private int clienteId;
    private int cantidadEntradas;
    private EstadoReserva estado;

    public ReservaImpl(int funcionId, int clienteId, int cantidadEntradas) {
        this.funcionId = funcionId;
        this.clienteId = clienteId;
        this.cantidadEntradas = cantidadEntradas;
        this.estado = EstadoReserva.RESERVADA;
    }

    public ReservaImpl(int id, int funcionId, int clienteId, int cantidadEntradas, EstadoReserva estado) {
        this(funcionId, clienteId, cantidadEntradas);
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
    public int getCantidadEntradas() {
        return cantidadEntradas;
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
                + " - " + cantidadEntradas + " entrada(s) - " + estado;
    }
}
