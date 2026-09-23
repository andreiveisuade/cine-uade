package ar.uade.cine.model.candy;

import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    private TipoProducto tipo;

    private Dinero precio;

    private boolean disponible;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "combo_item", joinColumns = @JoinColumn(name = "combo_id"))
    private List<ItemCombo> componentes = new ArrayList<>();

    protected Producto() {
    }

    public Producto(String nombre, TipoProducto tipo, Dinero precio) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.disponible = true;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoProducto getTipo() {
        return tipo;
    }

    public Dinero getPrecio() {
        return precio;
    }

    public boolean estaDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public void editar(String nombre, Dinero precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    public List<ItemCombo> getComponentes() {
        return new ArrayList<>(componentes);
    }

    public void agregarComponente(ItemCombo componente) {
        componentes.add(componente);
    }

    public boolean esCombo() {
        return tipo == TipoProducto.COMBO;
    }

    public Dinero getPrecioSuelto() {
        return Dinero.sumar(componentes.stream().map(ItemCombo::precioSuelto).toList());
    }

    public Dinero getAhorro() {
        return esCombo() ? getPrecioSuelto().menos(precio) : Dinero.CERO;
    }

    @Override
    public String toString() {
        String detalle = componentes.isEmpty() ? "" : " " + componentes;
        String baja = disponible ? "" : " (sin stock)";
        return "[" + id + "] " + nombre + " - $" + precio + detalle + baja;
    }
}
