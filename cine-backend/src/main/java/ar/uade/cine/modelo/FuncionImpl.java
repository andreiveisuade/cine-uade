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
    private double precio;

    public FuncionImpl(int peliculaId, int salaId, LocalDateTime inicio, double precio) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.inicio = inicio;
        this.precio = precio;
    }

    public FuncionImpl(int id, int peliculaId, int salaId, LocalDateTime inicio, double precio) {
        this(peliculaId, salaId, inicio, precio);
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
    public double getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + inicio + " ($" + precio + ")";
    }
}
