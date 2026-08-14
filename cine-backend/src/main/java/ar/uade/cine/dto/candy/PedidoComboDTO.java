package ar.uade.cine.dto.candy;

import java.util.Map;

/** {@code componentes} es id de producto a cuántas unidades trae el combo. */
public record PedidoComboDTO(String nombre, Double precio, Map<Integer, Integer> componentes) {
}
