package ar.uade.cine.dominio.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;

/** Un porcentaje del subtotal: el "miércoles 30% off". */
public class PromocionPorcentaje extends PromocionBase {

    private final double porcentaje;

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
    public double calcularDescuento(List<Entrada> entradas) {
        double subtotal = entradas.stream().mapToDouble(Entrada::precio).sum();
        return topear(redondear(subtotal * porcentaje / 100), entradas);
    }

    /** A dos decimales, por el mismo arrastre de error que se redondea en el precio. */
    private static double redondear(double monto) {
        return Math.round(monto * 100) / 100.0;
    }
}
