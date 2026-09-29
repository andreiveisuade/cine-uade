package ar.uade.cine.dto.catalogos;

// Un tipo de sala de GET /api/tipos-sala, con su recargo sobre el precio y si admite 3D (R8).
public record TipoSalaVistaDTO(String nombre, double multiplicador, boolean soportaTresD) {
}
