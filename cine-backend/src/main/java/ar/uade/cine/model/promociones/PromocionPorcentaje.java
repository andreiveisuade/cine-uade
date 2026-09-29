package ar.uade.cine.model.promociones;

import java.util.List;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

// Descuenta un porcentaje del subtotal, de 1 a 99; Polimorfismo sobre Promocion.
@Entity
@DiscriminatorValue("PORCENTAJE")
@Getter
public class PromocionPorcentaje extends Promocion {

    // Sin columnDefinition, Hibernate espera FLOAT y `validate` corta el arranque.
    @Column(columnDefinition = "DECIMAL(5,2)")
    private double porcentaje;

    protected PromocionPorcentaje() {
    }

    public PromocionPorcentaje(String nombre, double porcentaje, CondicionesPromocion condiciones) {
        super(nombre, condiciones);
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new IllegalArgumentException("El porcentaje tiene que estar entre 1 y 99");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.PORCENTAJE;
    }

    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        return topear(subtotalDe(entradas).porcentaje(porcentaje), entradas);
    }
}
