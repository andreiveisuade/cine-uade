package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Producto(int id, String nombre, String tipo, double precio, boolean disponible, boolean esCombo,
                       List<ItemCombo> componentes) {
}
