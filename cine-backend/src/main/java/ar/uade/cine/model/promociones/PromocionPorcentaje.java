package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PORCENTAJE")
public class PromocionPorcentaje extends Promocion {

    // Sin columnDefinition, Hibernate espera FLOAT y `validate` corta el arranque.
    @Column(columnDefinition = "DECIMAL(5,2)")
    private double porcentaje;

    protected PromocionPorcentaje() {
    }

    public PromocionPorcentaje(String nombre, double porcentaje, LocalDate vigenciaDesde,
                               LocalDate vigenciaHasta, Set<DayOfWeek> diasSemana,
                               LocalTime horaDesde, LocalTime horaHasta, Set<MedioPago> mediosPago) {
        super(nombre, vigenciaDesde, vigenciaHasta, diasSemana, horaDesde, horaHasta, mediosPago);
        this.porcentaje = porcentaje;
    }

    public double getPorcentaje() {
        return porcentaje;
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
