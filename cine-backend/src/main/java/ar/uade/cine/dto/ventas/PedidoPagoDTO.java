package ar.uade.cine.dto.ventas;

// Sin monto a propósito: sale del total congelado en la reserva.
public record PedidoPagoDTO(String medio, String codigoAutorizacion) {
}
