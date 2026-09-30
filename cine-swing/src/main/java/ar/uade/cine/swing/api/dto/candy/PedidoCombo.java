package ar.uade.cine.swing.api.dto.candy;

import java.util.Map;

// Lo que Swing manda al armar un combo de la carta; si falta nombre o precio, el mensaje lo da el gestor.
// productoId → cantidad.
public record PedidoCombo(String nombre, Double precio, Map<Integer, Integer> componentes) {
}
