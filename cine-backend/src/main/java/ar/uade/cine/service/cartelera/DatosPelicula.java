package ar.uade.cine.service.cartelera;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

// Datos de una película para alta o edición, de la API o de TMDB; un record en vez de doce parámetros.
// Campos objeto: en una edición null es "no lo mandé", distinto de cero o false.
public record DatosPelicula(String titulo, Integer duracionMinutos, List<Genero> generos,
                            Clasificacion clasificacion, String director, String sinopsis,
                            Integer anio, String idiomaOriginal, String posterUrl,
                            Boolean enCartelera, Double puntaje, Integer votos) {

    public static DatosPelicula deAlta(String titulo, int duracionMinutos, List<Genero> generos,
                                       Clasificacion clasificacion) {
        return con().titulo(titulo).duracion(duracionMinutos).generos(generos).clasificacion(clasificacion).armar();
    }

    // Builder: cada dato se nombra en vez de ir por posición.
    public static DatosPeliculaBuilder con() {
        return new DatosPeliculaBuilder();
    }
}
