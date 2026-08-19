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

/**
 * Algo que se vende en el candy. Un combo es también un Producto —tiene precio y se vende
 * como una unidad— y no una entidad aparte: lo único que lo distingue es que además sabe qué
 * trae adentro.
 *
 * <p>Por eso las dos claves de {@code combo_item} apuntan a esta misma tabla: un combo se
 * puede armar con cualquier otro producto.
 */
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

    /** Qué trae el combo. Lista vacía en un producto suelto. */
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

    public void setId(int id) {
        this.id = id;
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

    public void setPrecio(Dinero precio) {
        this.precio = precio;
    }

    /**
     * Si se sigue ofreciendo. Un producto no se borra: puede estar en compras viejas, y
     * borrarlo dejaría esos tickets apuntando a la nada.
     */
    public boolean estaDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    /** Copia defensiva: nadie modifica la lista interna desde afuera. */
    public List<ItemCombo> getComponentes() {
        return new ArrayList<>(componentes);
    }

    public void agregarComponente(ItemCombo componente) {
        componentes.add(componente);
    }

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
