package ar.uade.cine.dominio.candy;

import java.util.ArrayList;
import java.util.List;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Igual que PeliculaImpl: el constructor corto da de alta un producto nuevo (sin id,
 * disponible por defecto); el que recibe id y disponible es el que usa el DAO para
 * reconstruirlo desde la base.
 */
public class ProductoImpl implements Producto {

    private int id;
    private final String nombre;
    private final TipoProducto tipo;
    private Dinero precio;
    private boolean disponible;
    private final List<ItemCombo> componentes = new ArrayList<>();

    public ProductoImpl(String nombre, TipoProducto tipo, Dinero precio) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.disponible = true;
    }

    public ProductoImpl(int id, String nombre, TipoProducto tipo, Dinero precio, boolean disponible) {
        this(nombre, tipo, precio);
        this.id = id;
        this.disponible = disponible;
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
    public TipoProducto getTipo() {
        return tipo;
    }

    @Override
    public Dinero getPrecio() {
        return precio;
    }

    @Override
    public void setPrecio(Dinero precio) {
        this.precio = precio;
    }

    @Override
    public boolean estaDisponible() {
        return disponible;
    }

    @Override
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    @Override
    public List<ItemCombo> getComponentes() {
        return new ArrayList<>(componentes);
    }

    @Override
    public void agregarComponente(ItemCombo componente) {
        componentes.add(componente);
    }

    @Override
    public boolean esCombo() {
        return tipo == TipoProducto.COMBO;
    }

    @Override
    public String toString() {
        String detalle = componentes.isEmpty() ? "" : " " + componentes;
        String baja = disponible ? "" : " (sin stock)";
        return "[" + id + "] " + nombre + " - $" + precio + detalle + baja;
    }
}
