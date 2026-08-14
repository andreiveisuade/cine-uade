package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Un producto de la carta. Un combo es un producto que además dice qué trae: por eso
 * {@code componentes} viaja acá y no en un endpoint aparte.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductoVistaDTO(int id, String nombre, String tipo, double precio,
                            boolean disponible, boolean esCombo,
                            List<ItemComboVistaDTO> componentes) {
}
