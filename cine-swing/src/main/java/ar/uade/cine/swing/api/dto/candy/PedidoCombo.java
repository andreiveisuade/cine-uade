package ar.uade.cine.swing.api.dto.candy;

import java.util.Map;

// productoId → cantidad.
public record PedidoCombo(String nombre, Double precio, Map<Integer, Integer> componentes) {
}
