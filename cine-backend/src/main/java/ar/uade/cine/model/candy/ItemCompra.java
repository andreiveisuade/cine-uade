package ar.uade.cine.model.candy;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

// Renglón de una venta del candy; Experto: exige cantidad y stock, y congela nombre, precio y ahorro.
// Nombre, precio y ahorro se congelan al vender para que el ticket no cambie: si después se
// edita el combo, la compra vieja sigue diciendo lo que se cobró y lo que se ahorró.
@Entity
@Table(name = "item_compra")
@Getter
@Accessors(fluent = true)
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter(AccessLevel.NONE)
    private int id;

    // EAGER a propósito, como las colecciones: el id del producto lo leen las vistas, ya
    // fuera de la transacción.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private String nombre;

    private int cantidad;

    @Column(name = "precio_unitario")
    private Dinero precioUnitario;

    @Column(name = "ahorro_unitario")
    @Getter(AccessLevel.NONE)
    private Dinero ahorroUnitario;

    protected ItemCompra() {
    }

    // Solo lo crea CompraCandy. La cantidad llega como Integer porque viene del pedido: un null
    // tiene su propio mensaje en vez de un 500 por el unboxing.
    ItemCompra(Producto producto, Integer cantidad) {
        if (cantidad == null) {
            throw new IllegalArgumentException("Falta la cantidad de " + producto.getNombre());
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de " + producto.getNombre()
                    + " tiene que ser mayor a cero");
        }
        if (!producto.estaDisponible()) {
            throw new IllegalArgumentException(producto.getNombre() + " no está disponible");
        }
        this.producto = producto;
        this.nombre = producto.getNombre();
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        this.ahorroUnitario = producto.getAhorro();
    }

    public Dinero getSubtotal() {
        return precioUnitario.por(cantidad);
    }

    public Dinero getAhorro() {
        return ahorroUnitario.por(cantidad);
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
