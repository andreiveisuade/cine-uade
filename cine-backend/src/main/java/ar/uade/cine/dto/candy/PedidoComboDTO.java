package ar.uade.cine.dto.candy;

import java.util.Map;

public record PedidoComboDTO(String nombre, Double precio, Map<Integer, Integer> componentes) {
}
