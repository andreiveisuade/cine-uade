package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

public record PeliculaElegida(int id, String titulo, double puntaje, int duracionMinutos, List<String> generos,
                              int pases) {
}
