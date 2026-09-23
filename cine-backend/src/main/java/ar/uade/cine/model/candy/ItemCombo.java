package ar.uade.cine.model.candy;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Producto y cantidad dentro de un combo; permite comparar el combo contra comprarlo suelto.
 * {@code @Embeddable} porque no tiene identidad propia. El producto va por objeto porque el
 * ticket necesita su nombre.
 */
@Embeddable
public class ItemCombo {

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private int cantidad;

    protected ItemCombo() {
    }

    public ItemCombo(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Producto producto() {
        return producto;
    }

    public String nombre() {
        return producto.getNombre();
    }

    public Dinero precioSuelto() {
        return producto.getPrecio().por(cantidad);
    }

    public int cantidad() {
        return cantidad;
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre();
    }
}
