package ar.uade.cine.modelo;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Pelicula;

/**
 * Lo que identifica a la película va en el constructor; los datos de catálogo se cargan
 * después con setters. Un constructor de nueve parámetros sería imposible de leer y
 * facilísimo de invocar con los argumentos cambiados de orden.
 */
public class PeliculaImpl implements Pelicula {

    private int id;
    private String titulo;
    private int duracionMinutos;
    private Clasificacion clasificacion;
    private final List<Genero> generos = new ArrayList<>();

    private String director = "";
    private String sinopsis = "";
    private int anio;
    private String idiomaOriginal = "";
    private String posterUrl = "";
    private boolean enCartelera = true;

    /** Película nueva: todavía no tiene id, lo asigna la base al guardarla. */
    public PeliculaImpl(String titulo, int duracionMinutos, List<Genero> generos, Clasificacion clasificacion) {
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
        this.generos.addAll(generos);
    }

    /** Película que viene de la base: sus géneros se cargan con agregarGenero. */
    public PeliculaImpl(int id, String titulo, int duracionMinutos, Clasificacion clasificacion) {
        this.id = id;
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.clasificacion = clasificacion;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getTitulo() {
        return titulo;
    }

    @Override
    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    @Override
    public Clasificacion getClasificacion() {
        return clasificacion;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    @Override
    public List<Genero> getGeneros() {
        return new ArrayList<>(generos);
    }

    @Override
    public void agregarGenero(Genero genero) {
        if (!generos.contains(genero)) {
            generos.add(genero);
        }
    }

    @Override
    public String getDirector() {
        return director;
    }

    @Override
    public void setDirector(String director) {
        this.director = director;
    }

    @Override
    public String getSinopsis() {
        return sinopsis;
    }

    @Override
    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    @Override
    public int getAnio() {
        return anio;
    }

    @Override
    public void setAnio(int anio) {
        this.anio = anio;
    }

    @Override
    public String getIdiomaOriginal() {
        return idiomaOriginal;
    }

    @Override
    public void setIdiomaOriginal(String idiomaOriginal) {
        this.idiomaOriginal = idiomaOriginal;
    }

    @Override
    public String getPosterUrl() {
        return posterUrl;
    }

    @Override
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    @Override
    public boolean estaEnCartelera() {
        return enCartelera;
    }

    @Override
    public void setEnCartelera(boolean enCartelera) {
        this.enCartelera = enCartelera;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) "
                + clasificacion.getEtiqueta() + " " + generos
                + (enCartelera ? "" : " - fuera de cartelera");
    }
}
