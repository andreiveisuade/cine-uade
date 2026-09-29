package ar.uade.cine.model.promociones;

import java.time.LocalDate;
import java.util.List;

import ar.uade.cine.model.promociones.validacion.ValidadorPromocion;
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

    // Solo la crea TipoPromocion.PORCENTAJE (Factory Method) o un test. Double porque viene del pedido:
    // que falte lo dice el modelo.
    public PromocionPorcentaje(String nombre, Double porcentaje, CondicionesPromocion condiciones, LocalDate hoy) {
        super(nombre, condiciones, hoy);
        this.porcentaje = ValidadorPromocion.porcentaje(porcentaje);
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.PORCENTAJE;
    }

    @Override
    public ParametrosPromocion getParametros() {
        return ParametrosPromocion.dePorcentaje(porcentaje);
    }

    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        return topear(subtotalDe(entradas).porcentaje(porcentaje), entradas);
    }
}
