package ar.uade.cine.dto.programaciones;

// Una función que propone la grilla automática (/api/grilla), con título y sala para mostrarla sin buscar.
public record PaseSugeridoDTO(int peliculaId, String titulo, int salaId, String sala,
                              String inicio, int duracionMinutos) {
}
