package ar.uade.cine.model.ventas;

/**
 * Eje del cobro de una {@link Reserva}, en un solo sentido. El ingreso al cine es otro eje
 * ({@code ingresadaEn}): una reserva PAGADA puede no estar usada todavía.
 */
public enum EstadoReserva {

    RESERVADA,
    PAGADA,
    CANCELADA,

    /** R17. Distinta de CANCELADA: al cliente no se le muestra "cancelada" algo que no canceló. */
    EXPIRADA
}
