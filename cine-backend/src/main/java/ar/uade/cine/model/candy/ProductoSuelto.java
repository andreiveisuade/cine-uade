package ar.uade.cine.model.candy;

import ar.uade.cine.model.candy.validacion.ValidadorProducto;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Pochoclos, bebida o golosina por unidad; Composite (Leaf): su precio suelto es el propio y no ahorra.
@Entity
@DiscriminatorValue("SUELTO")
public class ProductoSuelto extends Producto {

    protected ProductoSuelto() {
    }

    // Va antes que el nombre: un combo sin nombre se rechaza por ser combo; Java evalúa el argumento primero
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
