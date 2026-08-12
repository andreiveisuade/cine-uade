package ar.uade.cine.interfaces;

import java.util.List;

import ar.uade.cine.modelo.EstadoReserva;

public interface Reserva {

    int getId();

    void setId(int id);

    int getFuncionId();

    int getClienteId();

    /** Una entrada por butaca elegida. */
    List<Entrada> getEntradas();

    /** Lo usa el DAO al reconstruir la reserva desde la base. */
    void agregarEntrada(Entrada entrada);

    /** Derivada de las entradas: no se guarda por separado. */
    int getCantidadEntradas();

    EstadoReserva getEstado();

    /** Pasa de RESERVADA a PAGADA o CANCELADA. */
    void setEstado(EstadoReserva estado);
}
