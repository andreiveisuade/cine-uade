package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Un porcentaje del subtotal: el "miércoles 30% off". */
@Entity
@DiscriminatorValue("PORCENTAJE")
public class PromocionPorcentaje extends Promocion {

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
        // Sin redondeo propio: Dinero ya trabaja en centavos enteros. Este metodo
        // tenia su copia del Math.round de CalculadoraPrecio, en otro paquete.
        return topear(subtotalDe(entradas).porcentaje(porcentaje), entradas);
    }
}
