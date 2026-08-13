package ar.uade.cine.dominio.ventas;

/**
 * Estados por los que pasa una {@link Reserva}, siempre en un solo sentido: nace
 * RESERVADA y de ahí avanza a PAGADA, CANCELADA o EXPIRADA. Cancelar una ya pagada no
 * está contemplado acá —hace falta una devolución, que es otro circuito.
 *
 * <p>Es el eje del <strong>cobro</strong>. El ingreso al cine es otro eje y no vive acá:
 * lo lleva {@code Reserva.ingresadaEn}, porque una reserva puede estar PAGADA y todavía
 * no usada, y mezclarlos obligaría a revisar cada regla que mira el estado.
 */
public enum EstadoReserva {

    RESERVADA,
    PAGADA,
    CANCELADA,

    /**
     * Nadie la pagó dentro de la ventana y sus butacas volvieron a la venta. Es distinta
     * de CANCELADA a propósito: en el historial del cliente no se le puede mostrar
     * "cancelada" a algo que él no canceló.
     */
    EXPIRADA
}
