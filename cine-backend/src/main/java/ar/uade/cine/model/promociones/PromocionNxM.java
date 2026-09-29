package ar.uade.cine.model.promociones;

import java.util.Comparator;
import java.util.List;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

// Lleva N y paga M: regala las N-M más baratas por cada N entradas; Polimorfismo sobre Promocion.
@Entity
@DiscriminatorValue("NXM")
@Getter
public class PromocionNxM extends Promocion {

    private int lleva;
    private int paga;

    protected PromocionNxM() {
    }

    public PromocionNxM(String nombre, int lleva, int paga, CondicionesPromocion condiciones) {
        super(nombre, condiciones);
        // Un 2x2 no descuenta y un 2x3 cobraría de más.
        if (lleva <= paga || paga <= 0) {
            throw new DatoInvalido("En un NxM hay que llevar más de lo que se paga");
        }
        this.lleva = lleva;
        this.paga = paga;
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
