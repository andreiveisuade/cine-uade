package ar.uade.cine.dto.candy;

/** Alta de un producto suelto. Un combo va por su propio endpoint. */
public record PedidoProductoDTO(String nombre, String tipo, Double precio) {
}
