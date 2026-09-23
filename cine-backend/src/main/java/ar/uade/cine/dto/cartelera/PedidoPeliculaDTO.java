package ar.uade.cine.dto.cartelera;

import java.util.List;

/**
 * Alta y edición. En el PUT un null es "no lo mandé"; en el alta dispara el error de dato
 * faltante del gestor.
 */
public record PedidoPeliculaDTO(String titulo, Integer duracionMinutos, List<String> generos,
                             String clasificacion, String director, String sinopsis, Integer anio,
                             String idiomaOriginal, String posterUrl, Boolean enCartelera,
                             Double puntaje, Integer votos) {
}
