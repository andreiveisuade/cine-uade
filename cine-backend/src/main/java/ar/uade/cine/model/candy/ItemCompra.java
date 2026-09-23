package ar.uade.cine.model.candy;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de una compra. Nombre y precio se congelan al vender para que el ticket no cambie.
 * Entidad y no {@code @Embeddable} como {@link ItemCombo} porque la tabla le da clave propia.
 */
@Entity
@Table(name = "item_compra")
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private String nombre;

    private int cantidad;

    @Column(name = "precio_unitario")
    private Dinero precioUnitario;

    protected ItemCompra() {
    }

    public ItemCompra(Producto producto, int cantidad, Dinero precioUnitario) {
        this.producto = producto;
        this.nombre = producto.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Producto producto() {
        return producto;
    }

    public String nombre() {
        return nombre;
    }

    public int cantidad() {
        return cantidad;
    }

    public Dinero precioUnitario() {
        return precioUnitario;
    }

    public Dinero getSubtotal() {
        return precioUnitario.por(cantidad);
    }

    public Dinero getAhorro() {
        return producto.getAhorro().por(cantidad);
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
