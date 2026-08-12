package ar.uade.cine.interfaces;

import ar.uade.cine.modelo.EstadoReserva;

public interface Reserva {

    int getId();

    void setId(int id);

    int getFuncionId();

    int getClienteId();

    int getCantidadEntradas();

    EstadoReserva getEstado();

    /** Pasa de RESERVADA a PAGADA o CANCELADA. */
    void setEstado(EstadoReserva estado);
}
