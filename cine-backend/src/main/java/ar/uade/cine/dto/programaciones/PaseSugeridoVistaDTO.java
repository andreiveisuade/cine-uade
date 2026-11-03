package ar.uade.cine.dto.programaciones;

// Un pase que propone la grilla automática (/api/grilla), con título, sala y formato para mostrarlo sin buscar.
public record PaseSugeridoVistaDTO(int peliculaId, String titulo, int salaId, String sala,
                                   String inicio, int duracionMinutos, String proyeccion) {
}
