package ar.uade.cine.dto.programaciones;

public record PaseSugeridoDTO(int peliculaId, String titulo, int salaId, String sala,
                              String inicio, int duracionMinutos) {
}
