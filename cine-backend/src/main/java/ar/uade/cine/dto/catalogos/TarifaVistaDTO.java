package ar.uade.cine.dto.catalogos;

// Una tarifa de entrada de GET /api/tarifas y si se acredita con carnet; el precio lo calcula el backend.
public record TarifaVistaDTO(String nombre, boolean requiereAcreditacion) {
}
