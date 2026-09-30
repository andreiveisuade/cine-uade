package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

// Una película que eligió el planificador para la semana, con su puntaje y cuántos pases le tocaron.
public record PeliculaElegida(int id, String titulo, double puntaje, int duracionMinutos, List<String> generos,
                              int pases) {
}
