package ar.uade.cine.dto.candy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Un producto o combo de la carta (/api/candy/productos); componentes viene vacío si no es combo.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductoVistaDTO(int id, String nombre, String tipo, double precio,
                            boolean disponible, boolean esCombo,
                            List<ItemComboVistaDTO> componentes) {
}
