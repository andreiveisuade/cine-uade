package ar.uade.cine.modelo;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.interfaces.Pelicula;

public class PeliculaImpl implements Pelicula {

    private int id;
    private String titulo;
    private int duracionMinutos;
    private final List<Genero> generos = new ArrayList<>();

    /** Película nueva: todavía no tiene id, lo asigna la base al guardarla. */
    public PeliculaImpl(String titulo, int duracionMinutos, List<Genero> generos) {
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.generos.addAll(generos);
    }

    /** Película que viene de la base: sus géneros se cargan con agregarGenero. */
    public PeliculaImpl(int id, String titulo, int duracionMinutos) {
        this.id = id;
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
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
    public String toString() {
        return "[" + id + "] " + titulo + " (" + duracionMinutos + " min) " + generos;
    }
}
