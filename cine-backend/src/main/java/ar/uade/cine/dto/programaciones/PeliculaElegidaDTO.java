package ar.uade.cine.dto.programaciones;

import java.util.List;

public record PeliculaElegidaDTO(int id, String titulo, double puntaje, int duracionMinutos,
                                 List<String> generos, int pases) {
}
