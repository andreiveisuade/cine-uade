package ar.uade.cine.dto.candy;

/** Qué trae un combo: el producto que incluye y cuántas unidades. */
public record ItemComboVistaDTO(int productoId, String nombre, int cantidad) {
}
