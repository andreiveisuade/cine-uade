package ar.uade.cine.dto.candy;

/** Una línea de la venta, con el precio que tenía el producto en ese momento. */
public record ItemCompraVistaDTO(int productoId, String nombre, int cantidad,
                              double precioUnitario, double subtotal) {
}
