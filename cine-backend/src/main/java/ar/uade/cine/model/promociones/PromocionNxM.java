package ar.uade.cine.model.promociones;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import ar.uade.cine.model.promociones.validacion.ValidadorPromocion;
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

    // Solo la crea TipoPromocion.NXM (Factory Method) o un test. Integer porque vienen del pedido: que
    // falte uno lo dice el modelo, en vez de un 500 por el unboxing.
    public PromocionNxM(String nombre, Integer lleva, Integer paga, CondicionesPromocion condiciones,
                        LocalDate hoy) {
        super(nombre, condiciones, hoy);
        ValidadorPromocion.exigirNxM(lleva, paga);
        this.lleva = lleva;
        this.paga = paga;
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.NXM;
    }

    @Override
    public ParametrosPromocion getParametros() {
        return ParametrosPromocion.deNxM(lleva, paga);
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
