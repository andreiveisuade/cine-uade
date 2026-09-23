package ar.uade.cine.dto.ventas;

/**
 * Sin monto a propósito: sale del total congelado en la reserva, así nadie cobra $100 una
 * reserva de $16.000.
 */
public record PedidoPagoDTO(String medio, String codigoAutorizacion) {
}
