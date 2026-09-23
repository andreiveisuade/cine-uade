package ar.uade.cine.dto.candy;

public record ItemCompraVistaDTO(int productoId, String nombre, int cantidad,
                              double precioUnitario, double subtotal) {
}
