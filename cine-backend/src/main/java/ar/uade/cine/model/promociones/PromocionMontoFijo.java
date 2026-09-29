package ar.uade.cine.model.promociones;

import java.util.List;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

// Descuenta un monto fijo por compra, topeado al subtotal; Polimorfismo sobre Promocion.
@Entity
@DiscriminatorValue("MONTO_FIJO")
@Getter
public class PromocionMontoFijo extends Promocion {

    private Dinero monto;

    protected PromocionMontoFijo() {
    }

    public PromocionMontoFijo(String nombre, Dinero monto, CondicionesPromocion condiciones) {
        super(nombre, condiciones);
        if (monto == null || !monto.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El monto del descuento debe ser mayor a cero");
        }
        this.monto = monto;
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.MONTO_FIJO;
    }

    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        return topear(monto, entradas);
    }
}
