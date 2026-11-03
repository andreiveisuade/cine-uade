package ar.uade.cine.dto.catalogos;

// Un tipo de sala de GET /api/tipos-sala: multiplicador, si da 3D (R8), si es premium y qué le toca en la grilla.
public record TipoSalaVistaDTO(String nombre, double multiplicador, boolean soportaTresD, boolean premium,
                               String enLaGrilla) {
}
