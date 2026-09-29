package ar.uade.cine.model.candy;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.experimental.Accessors;

@Embeddable
@Getter
@Accessors(fluent = true)
public class ItemCombo {

    // EAGER a propósito, como las colecciones: el nombre del componente lo leen las vistas,
    // ya fuera de la transacción.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private int cantidad;

    protected ItemCombo() {
    }

    public ItemCombo(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public String nombre() {
        return producto.getNombre();
    }

    public Dinero precioSuelto() {
        return producto.getPrecio().por(cantidad);
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre();
    }
}
