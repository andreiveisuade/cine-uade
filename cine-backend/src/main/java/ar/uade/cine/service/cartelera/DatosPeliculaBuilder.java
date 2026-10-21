package ar.uade.cine.service.cartelera;

import java.util.List;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;

// Arma un DatosPelicula nombrando cada dato; lo no nombrado viaja null, que en edición es "no lo mandé".
// Patrón Builder (GoF): trece componentes posicionales se podían cruzar sin error de compilación.
// Solo para este record sin reglas: las entidades validan en su constructor.
public final class DatosPeliculaBuilder {

    private String titulo;
    private Integer duracionMinutos;
    private List<Genero> generos;
    private Clasificacion clasificacion;
    private String director;
    private String sinopsis;
    private Integer anio;
    private String idiomaOriginal;
    private String posterUrl;
    private Boolean enCartelera;
    private Double puntaje;
    private Integer votos;
    private String trailerYoutube;

    DatosPeliculaBuilder() {
    }

    public DatosPeliculaBuilder titulo(String titulo) {
        this.titulo = titulo;
        return this;
    }

    public DatosPeliculaBuilder duracion(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
        return this;
    }

    public DatosPeliculaBuilder generos(List<Genero> generos) {
        this.generos = generos;
        return this;
    }

    public DatosPeliculaBuilder clasificacion(Clasificacion clasificacion) {
        this.clasificacion = clasificacion;
        return this;
    }

    public DatosPeliculaBuilder director(String director) {
        this.director = director;
        return this;
    }

    public DatosPeliculaBuilder sinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
        return this;
    }

    public DatosPeliculaBuilder anio(Integer anio) {
        this.anio = anio;
        return this;
    }

    public DatosPeliculaBuilder idiomaOriginal(String idiomaOriginal) {
        this.idiomaOriginal = idiomaOriginal;
        return this;
    }

    public DatosPeliculaBuilder posterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
        return this;
    }

    public DatosPeliculaBuilder enCartelera(Boolean enCartelera) {
        this.enCartelera = enCartelera;
        return this;
    }

    public DatosPeliculaBuilder puntaje(Double puntaje) {
        this.puntaje = puntaje;
        return this;
    }

    public DatosPeliculaBuilder votos(Integer votos) {
        this.votos = votos;
        return this;
    }

    public DatosPeliculaBuilder trailerYoutube(String trailerYoutube) {
        this.trailerYoutube = trailerYoutube;
        return this;
    }

    public DatosPelicula armar() {
        return new DatosPelicula(titulo, duracionMinutos, generos, clasificacion, director, sinopsis,
                anio, idiomaOriginal, posterUrl, enCartelera, puntaje, votos, trailerYoutube);
    }
}
