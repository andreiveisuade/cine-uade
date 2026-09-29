package ar.uade.cine.model.candy;

import ar.uade.cine.model.candy.validacion.ValidadorCombo;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.experimental.Accessors;

// Componente de un combo con su cantidad; ValidadorCombo exige de 1 a 20 unidades y que no sea otro combo.
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

    // Solo lo crea Producto.armarCombo. La cantidad llega como Integer porque viene del pedido: sin
    // chequear el null, el unboxing daría un 500. La regla es la de ItemCompra: es la misma cantidad.
    ItemCombo(Producto producto, Integer cantidad) {
        this.cantidad = ValidadorCombo.componente(producto, cantidad);
        this.producto = producto;
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
