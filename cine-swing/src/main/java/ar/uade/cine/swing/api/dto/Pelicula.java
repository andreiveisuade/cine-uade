package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Pelicula(int id, String titulo, int duracionMinutos, List<String> generos, String clasificacion,
                       String posterUrl, String director, int anio, String idiomaOriginal, String sinopsis,
                       boolean enCartelera, String estadoRevision) {
}
