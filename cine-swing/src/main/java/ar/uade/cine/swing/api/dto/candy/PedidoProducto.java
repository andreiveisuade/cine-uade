package ar.uade.cine.swing.api.dto.candy;

// Lo que Swing manda al dar de alta un producto suelto de la carta; tipo va por nombre de constante.
public record PedidoProducto(String nombre, String tipo, Double precio) {
}
