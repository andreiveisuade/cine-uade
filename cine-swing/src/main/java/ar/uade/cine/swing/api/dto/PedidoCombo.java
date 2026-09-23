package ar.uade.cine.swing.api.dto;

import java.util.Map;

// productoId → cantidad.
public record PedidoCombo(String nombre, Double precio, Map<Integer, Integer> componentes) {
}
