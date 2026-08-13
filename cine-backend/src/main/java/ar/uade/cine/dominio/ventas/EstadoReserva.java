package ar.uade.cine.dominio.ventas;

/**
 * Estados por los que pasa una {@link Reserva}, siempre en un solo sentido:
 * RESERVADA → PAGADA o RESERVADA → CANCELADA. Cancelar una ya pagada no está
 * contemplado acá —hace falta una devolución, que es otro circuito.
 */
public enum EstadoReserva {
    RESERVADA,
    PAGADA,
    CANCELADA
}
