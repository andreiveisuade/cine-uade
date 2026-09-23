package ar.uade.cine.dto.grilla;

import java.util.List;

public record PeliculaElegidaDTO(int id, String titulo, double puntaje, int duracionMinutos,
                                 List<String> generos, int pases) {
}
