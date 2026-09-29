package ar.uade.cine.dto.cartelera;

import java.util.List;

// Una película de /api/cartelera y /api/peliculas, también embebida en funciones, reservas y pagos.
public record PeliculaVistaDTO(int id, String titulo, int duracionMinutos, List<String> generos,
                               String clasificacion, String posterUrl, String director, int anio,
                               String idiomaOriginal, String sinopsis, boolean enCartelera,
                               String estadoRevision, double puntaje, int votos) {
}
