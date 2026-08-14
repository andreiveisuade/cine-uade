package ar.uade.cine.dto.ventas;

/**
 * Para abrir un checkout alcanza con el medio: la reserva va en la ruta y el monto lo
 * calcula el backend, igual que en el cobro. No lleva código de autorización porque es
 * justamente lo que todavía no existe — el checkout se abre para conseguirlo.
 */
public record PedidoCheckoutDTO(String medio) {
}
