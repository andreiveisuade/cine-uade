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

@Entity
@DiscriminatorValue("NXM")
public class PromocionNxM extends Promocion {

    private int lleva;
    private int paga;

    protected PromocionNxM() {
    }

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
