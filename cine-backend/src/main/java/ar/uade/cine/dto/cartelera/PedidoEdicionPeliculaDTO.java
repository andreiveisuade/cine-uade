package ar.uade.cine.dto.cartelera;

import java.util.List;

// Edición parcial: lo que no viaja queda como estaba, así que nada es obligatorio.
public record PedidoEdicionPeliculaDTO(String titulo, Integer duracionMinutos, List<String> generos,
                                       String clasificacion, String director, String sinopsis,
                                       Integer anio, String idiomaOriginal, String posterUrl,
                                       Boolean enCartelera, Double puntaje, Integer votos) {
}
