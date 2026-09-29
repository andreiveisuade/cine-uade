package ar.uade.cine.swing.api.dto.cartelera;

import java.util.List;

// Sirve para el alta y para la edición parcial: en el PUT, lo que va null no viaja y queda como estaba.
public record PedidoPelicula(String titulo, Integer duracionMinutos, List<String> generos, String clasificacion,
                             String director, String sinopsis, Integer anio, String idiomaOriginal,
                             String posterUrl, Boolean enCartelera) {

    public static PedidoPelicula soloPublicacion(boolean enCartelera) {
        return new PedidoPelicula(null, null, null, null, null, null, null, null, null, enCartelera);
    }
}
