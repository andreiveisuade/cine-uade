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
 * Una línea de la compra: qué producto, cuántos y a cuánto. El precio se congela igual que
 * en las entradas, para que el ticket emitido siga siendo válido aunque después cambie la
 * lista de precios. El nombre se copia por lo mismo.
 *
 * <p>Es una entidad y no un {@code @Embeddable} —a diferencia de {@link ItemCombo}— porque
 * la tabla le da clave propia: la línea de una compra existe como fila con su id.
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

    /** Congelado al vender: el ticket no cambia si mañana el producto se renombra. */
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

    public int productoId() {
        return producto.getId();
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

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
