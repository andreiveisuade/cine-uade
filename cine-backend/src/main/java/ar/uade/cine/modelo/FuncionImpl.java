package ar.uade.cine.modelo;

import java.time.LocalDateTime;

import ar.uade.cine.interfaces.Funcion;

/**
 * Referencia a película y sala por id: el DAO trae el objeto completo cuando hace falta.
 */
public class FuncionImpl implements Funcion {

    private int id;
    private int peliculaId;
    private int salaId;
    private LocalDateTime inicio;
    private Idioma idioma;
    private Proyeccion proyeccion;
    private double precio;

    public FuncionImpl(int peliculaId, int salaId, LocalDateTime inicio,
                       Idioma idioma, Proyeccion proyeccion, double precio) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.inicio = inicio;
        this.idioma = idioma;
        this.proyeccion = proyeccion;
        this.precio = precio;
    }

    public FuncionImpl(int id, int peliculaId, int salaId, LocalDateTime inicio,
                       Idioma idioma, Proyeccion proyeccion, double precio) {
        this(peliculaId, salaId, inicio, idioma, proyeccion, precio);
        this.id = id;
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
    public int getPeliculaId() {
        return peliculaId;
    }

    @Override
    public int getSalaId() {
        return salaId;
    }

    @Override
    public LocalDateTime getInicio() {
        return inicio;
    }

    @Override
    public Idioma getIdioma() {
        return idioma;
    }

    @Override
    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    @Override
    public double getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + inicio
                + " - " + proyeccion + " " + idioma + " - desde $" + precio;
    }
}
