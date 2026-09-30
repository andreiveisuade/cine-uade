package ar.uade.cine.dto.candy;

// Un componente de un combo dentro de ProductoVistaDTO: qué producto trae y cuántas unidades.
public record ItemComboVistaDTO(int productoId, String nombre, int cantidad) {
}
