package ar.uade.cine.swing.api.dto.programaciones;

// Una función que propone el planificador, con título y sala para mostrarla sin buscarlas aparte.
public record PaseSugerido(int peliculaId, String titulo, int salaId, String sala, String inicio,
                           int duracionMinutos) {
}
