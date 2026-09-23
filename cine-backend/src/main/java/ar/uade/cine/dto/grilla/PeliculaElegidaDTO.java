package ar.uade.cine.dto.grilla;

import java.util.List;

/**
 * Una película del elenco con lo que justifica su elección: puntaje y géneros, no la ficha
 * completa.
 */
public record PeliculaElegidaDTO(int id, String titulo, double puntaje, int duracionMinutos,
                                 List<String> generos, int pases) {
}
