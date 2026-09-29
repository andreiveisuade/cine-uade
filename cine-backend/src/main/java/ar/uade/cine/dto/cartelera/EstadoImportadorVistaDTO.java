package ar.uade.cine.dto.cartelera;

// Si el importador puede correr (GET /api/importaciones/estado); solo mira el token, no consulta TMDB.
public record EstadoImportadorVistaDTO(boolean disponible, String detalle) {
}
