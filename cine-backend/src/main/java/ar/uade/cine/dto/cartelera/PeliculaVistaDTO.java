package ar.uade.cine.dto.cartelera;

import java.util.List;

/** Una película para el front: los géneros y la clasificación ya pasados a texto. */
public record PeliculaVistaDTO(int id, String titulo, int duracionMinutos, List<String> generos,
                               String clasificacion, String posterUrl, String director, int anio,
                               String idiomaOriginal, String sinopsis, boolean enCartelera,
                               String estadoRevision) {
}
