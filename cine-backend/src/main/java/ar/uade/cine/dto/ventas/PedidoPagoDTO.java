package ar.uade.cine.dto.ventas;

/**
 * No lleva monto a propósito: sale del total de la reserva, que a su vez es lo que se
 * congeló en cada entrada al reservar. Si el importe fuera un dato de entrada, se podría
 * cobrar $100 una reserva de $16.000.
 */
public record PedidoPagoDTO(String medio, String codigoAutorizacion) {
}
