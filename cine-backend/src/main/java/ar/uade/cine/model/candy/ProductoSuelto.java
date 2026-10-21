package ar.uade.cine.model.candy;

import ar.uade.cine.model.candy.validacion.ValidadorProducto;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Pochoclos, bebida o golosina que se vende por unidad. Patrón Composite (Leaf): no trae nada, así que su
// precio suelto es el propio y no ahorra.
@Entity
@DiscriminatorValue("SUELTO")
public class ProductoSuelto extends Producto {

    protected ProductoSuelto() {
    }

    // Que no sea combo va antes que el nombre: un combo sin nombre se rechaza por ser combo, que es el error
    // de fondo. Por eso se chequea en el argumento, que Java evalúa antes del constructor de Producto.
    public ProductoSuelto(String nombre, TipoProducto tipo, Dinero precio) {
        super(nombre, ValidadorProducto.exigirQueNoSeaCombo(tipo), precio);
    }

    @Override
    public Dinero getPrecioSuelto() {
        return getPrecio();
    }

    @Override
    public Dinero getAhorro() {
        return Dinero.CERO;
    }
}
