package ar.uade.cine.swing.api.dto.candy;

import java.util.List;

// Un producto o combo de la carta del candy como lo lee Swing; componentes viene vacío si no es combo.
public record Producto(int id, String nombre, String tipo, double precio, boolean disponible, boolean esCombo,
                       List<ItemCombo> componentes) {
}
