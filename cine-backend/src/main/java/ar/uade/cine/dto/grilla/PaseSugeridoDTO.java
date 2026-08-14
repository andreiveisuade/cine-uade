package ar.uade.cine.dto.grilla;

/** Una función propuesta que todavía no existe: qué película, en qué sala y cuándo. */
public record PaseSugeridoDTO(int peliculaId, String titulo, int salaId, String sala,
                              String inicio, int duracionMinutos) {
}
