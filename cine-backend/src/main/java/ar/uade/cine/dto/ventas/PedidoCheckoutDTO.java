package ar.uade.cine.dto.ventas;

/**
 * Solo el medio: la reserva va en la ruta, el monto lo calcula el backend y el código de
 * autorización es lo que el checkout sale a conseguir.
 */
public record PedidoCheckoutDTO(String medio) {
}
