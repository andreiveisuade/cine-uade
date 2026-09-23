package ar.uade.cine.dto.candy;

/** Con el precio que tenía el producto al venderse. */
public record ItemCompraVistaDTO(int productoId, String nombre, int cantidad,
                              double precioUnitario, double subtotal) {
}
