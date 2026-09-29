package ar.uade.cine.swing.api.dto.candy;

// Un renglón de una venta de candy; el subtotal viene congelado del backend, Swing no lo recalcula.
public record ItemCompra(String nombre, int cantidad, double subtotal) {
}
