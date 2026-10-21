package ar.uade.cine.model.candy;

import ar.uade.cine.model.candy.validacion.ValidadorCompraCandy;
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

// Renglón de una venta del candy; congela nombre, precio y ahorro. Cantidad y stock: ValidadorCompraCandy.
// Se congela al vender para que el ticket no cambie si después se edita el combo
@Entity
@Table(name = "item_compra")
@Getter
@Accessors(fluent = true)
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter(AccessLevel.NONE)
    private int id;

    // EAGER: las vistas leen el id del producto fuera de la transacción
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

    // Integer porque viene del pedido: el null tiene su propio mensaje, no un 500
    ItemCompra(Producto producto, Integer cantidad) {
        this.cantidad = ValidadorCompraCandy.renglon(producto, cantidad);
        this.producto = producto;
        this.nombre = producto.getNombre();
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
