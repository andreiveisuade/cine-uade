package ar.uade.cine.model.candy;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.experimental.Accessors;

// Componente de un combo con su cantidad; Experto: exige cantidad positiva y que no sea otro combo.
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
    // chequear el null, el unboxing daría un 500. Los textos son los de ItemCompra: es la misma cantidad.
    ItemCombo(Producto producto, Integer cantidad) {
        if (cantidad == null) {
            throw new DatoInvalido("Falta la cantidad de " + producto.getNombre());
        }
        if (cantidad <= 0) {
            throw new DatoInvalido("La cantidad de " + producto.getNombre()
                    + " tiene que ser mayor a cero");
        }
        if (producto.esCombo()) {
            throw new DatoInvalido("Un combo no puede contener otro combo: " + producto.getNombre());
        }
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
