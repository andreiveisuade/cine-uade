package ar.uade.cine.dto.catalogos;

// Una clasificación por edad de GET /api/clasificaciones, con la edad mínima que exige.
public record ClasificacionVistaDTO(String nombre, int edadMinima) {
}
