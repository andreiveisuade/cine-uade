package ar.uade.cine.model.promociones;

import java.time.LocalDate;
import java.util.List;

import ar.uade.cine.model.promociones.validacion.ValidadorPromocion;
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

    // Solo la crea TipoPromocion.MONTO_FIJO (Factory Method) o un test. El monto llega en pesos, como
    // viene del pedido, así «Falta el monto del descuento» lo dice el modelo.
    public PromocionMontoFijo(String nombre, Double monto, CondicionesPromocion condiciones, LocalDate hoy) {
        super(nombre, condiciones, hoy);
        this.monto = ValidadorPromocion.monto(monto);
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.MONTO_FIJO;
    }

    @Override
    public ParametrosPromocion getParametros() {
        return ParametrosPromocion.deMonto(monto.aPesos());
    }

    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        return topear(monto, entradas);
    }
}
