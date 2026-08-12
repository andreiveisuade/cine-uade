package ar.uade.cine.modelo;

import ar.uade.cine.interfaces.Sala;

public class SalaImpl implements Sala {

    private int id;
    private String nombre;
    private int capacidad;

    public SalaImpl(String nombre, int capacidad) {
        this.nombre = nombre;
        this.capacidad = capacidad;
    }

    public SalaImpl(int id, String nombre, int capacidad) {
        this(nombre, capacidad);
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
    public String getNombre() {
        return nombre;
    }

    @Override
    public int getCapacidad() {
        return capacidad;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre + " (" + capacidad + " lugares)";
    }
}
