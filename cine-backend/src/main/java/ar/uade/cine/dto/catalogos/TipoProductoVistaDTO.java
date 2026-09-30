package ar.uade.cine.dto.catalogos;

// Un tipo de producto de GET /api/tipos-producto; esCombo dice si el alta va por /api/candy/combos.
public record TipoProductoVistaDTO(String nombre, boolean esCombo) {
}
