package ar.uade.cine.model.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Llevás n, pagás m: el 2x1 de toda la vida.
 *
 * <p>Es la razón de que el descuento se modele como un monto sobre el conjunto. Las otras
 * dos se podrían haber escrito como un factor sobre cada butaca; esta no: "cada dos
 * entradas, una gratis" no existe a nivel de una entrada sola.
 */
@Entity
@DiscriminatorValue("NXM")
public class PromocionNxM extends Promocion {

    private int lleva;
    private int paga;

    public PromocionNxM(String nombre, int lleva, int paga, LocalDate vigenciaDesde,
                        LocalDate vigenciaHasta, Set<DayOfWeek> diasSemana,
                        LocalTime horaDesde, LocalTime horaHasta, Set<MedioPago> mediosPago) {
        super(nombre, vigenciaDesde, vigenciaHasta, diasSemana, horaDesde, horaHasta, mediosPago);
        this.lleva = lleva;
        this.paga = paga;
    }

    public int getLleva() {
        return lleva;
    }

    public int getPaga() {
        return paga;
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.NXM;
    }

    /**
     * Regala las más baratas, no las más caras: es lo que hace cualquier cine y lo que
     * el cliente espera. Con cinco entradas y un 2x1 entran dos grupos completos —cuatro
     * butacas— y la quinta se paga entera.
     */
    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        int grupos = entradas.size() / lleva;
        int gratis = grupos * (lleva - paga);
        if (gratis <= 0) {
            return Dinero.CERO;
        }
        Dinero descuento = Dinero.sumar(entradas.stream()
                .map(Entrada::precio)
                .sorted(Comparator.naturalOrder())
                .limit(gratis)
                .toList());
        return topear(descuento, entradas);
    }
}
