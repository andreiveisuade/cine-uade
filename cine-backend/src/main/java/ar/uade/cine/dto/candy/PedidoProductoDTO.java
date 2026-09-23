package ar.uade.cine.dto.candy;

/** Alta de un producto suelto; los combos van por su endpoint. */
public record PedidoProductoDTO(String nombre, String tipo, Double precio) {
}
