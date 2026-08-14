package ar.uade.cine.dto.cartelera;

import java.util.List;

/**
 * Sirve para el alta y para la edición: en el PUT todos los campos son opcionales.
 *
 * <p>Un campo ausente viaja en null a propósito: es lo que el gestor lee como "no lo
 * mandé". En el alta, ese mismo null es el que dispara el error de dato faltante, con el
 * mensaje del gestor y no con uno que invente la capa HTTP.
 */
public record PedidoPeliculaDTO(String titulo, Integer duracionMinutos, List<String> generos,
                             String clasificacion, String director, String sinopsis, Integer anio,
                             String idiomaOriginal, String posterUrl, Boolean enCartelera) {
}
