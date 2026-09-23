package ar.uade.cine.dto.cartelera;

import java.util.List;

public record PedidoPeliculaDTO(String titulo, Integer duracionMinutos, List<String> generos,
                             String clasificacion, String director, String sinopsis, Integer anio,
                             String idiomaOriginal, String posterUrl, Boolean enCartelera,
                             Double puntaje, Integer votos) {
}
