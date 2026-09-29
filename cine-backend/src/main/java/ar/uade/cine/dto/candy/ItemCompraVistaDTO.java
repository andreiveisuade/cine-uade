package ar.uade.cine.dto.candy;

// Un renglón de una venta de candy; el precio unitario es el congelado al vender, no el de la carta.
public record ItemCompraVistaDTO(int productoId, String nombre, int cantidad,
                                 double precioUnitario, double subtotal) {
}
