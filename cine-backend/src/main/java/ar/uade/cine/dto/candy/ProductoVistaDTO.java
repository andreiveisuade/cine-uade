package ar.uade.cine.dto.candy;

import java.util.List;

// Un producto o combo de la carta (/api/candy/productos); el suelto trae componentes vacío y ahorro 0.
public record ProductoVistaDTO(int id, String nombre, String tipo, double precio,
                               boolean disponible, boolean esCombo, double precioSuelto, double ahorro,
                               List<ItemComboVistaDTO> componentes, String imagenUrl) {
}
