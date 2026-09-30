package ar.uade.cine.dto.programaciones;

import java.util.List;

// Una película del elenco de la grilla automática (/api/grilla), con cuántos pases le tocaron.
public record PeliculaElegidaVistaDTO(int id, String titulo, double puntaje, int duracionMinutos,
                                      List<String> generos, int pases) {
}
