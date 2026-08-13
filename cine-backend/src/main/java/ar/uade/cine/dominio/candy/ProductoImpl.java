package ar.uade.cine.dominio.candy;

import java.util.ArrayList;
import java.util.List;

public class ProductoImpl implements Producto {

    private int id;
    private final String nombre;
    private final TipoProducto tipo;
    private double precio;
    private boolean disponible;
    private final List<ItemCombo> componentes = new ArrayList<>();

    public ProductoImpl(String nombre, TipoProducto tipo, double precio) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.disponible = true;
    }

    public ProductoImpl(int id, String nombre, TipoProducto tipo, double precio, boolean disponible) {
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
    public double getPrecio() {
        return precio;
    }

    @Override
    public void setPrecio(double precio) {
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
