package ar.uade.cine.dominio.ventas;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Butacas de una función a nombre de un cliente. Nace {@link EstadoReserva#RESERVADA}
 * y desde ahí solo avanza: a PAGADA cuando se cobra, o a CANCELADA cuando se libera sin
 * cobrar. No hay vuelta atrás entre esos dos estados finales.
 */
public interface Reserva {

    int getId();

    void setId(int id);

    int getFuncionId();

    int getClienteId();

    /** Cuándo se hizo. Sin esto no se puede ordenar el historial ni auditar una venta. */
    LocalDateTime getCreadaEn();

    /** Una entrada por butaca elegida. */
    List<Entrada> getEntradas();

    /** Lo usa el DAO al reconstruir la reserva desde la base. */
    void agregarEntrada(Entrada entrada);

    /** Derivada de las entradas: no se guarda por separado. */
    int getCantidadEntradas();

    /** Suma de lo cobrado por cada butaca. */
    double getTotal();

    EstadoReserva getEstado();

    /** Pasa de RESERVADA a PAGADA o CANCELADA. */
    void setEstado(EstadoReserva estado);
}
