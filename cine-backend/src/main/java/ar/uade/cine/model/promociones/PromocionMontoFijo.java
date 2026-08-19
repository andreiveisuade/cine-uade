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

/** Una cantidad fija de plata: el "$2000 off pagando con tal banco". */
@Entity
@DiscriminatorValue("MONTO_FIJO")
public class PromocionMontoFijo extends Promocion {

    private Dinero monto;

    protected PromocionMontoFijo() {
    }

    public PromocionMontoFijo(String nombre, Dinero monto, LocalDate vigenciaDesde,
                              LocalDate vigenciaHasta, Set<DayOfWeek> diasSemana,
                              LocalTime horaDesde, LocalTime horaHasta, Set<MedioPago> mediosPago) {
        super(nombre, vigenciaDesde, vigenciaHasta, diasSemana, horaDesde, horaHasta, mediosPago);
        this.monto = monto;
    }

    public Dinero getMonto() {
        return monto;
    }

    @Override
    public TipoPromocion getTipo() {
        return TipoPromocion.MONTO_FIJO;
    }

    /** Topeado al subtotal: un descuento de $2000 sobre una entrada de $1500 la deja en cero, no en negativo. */
    @Override
    public Dinero calcularDescuento(List<Entrada> entradas) {
        return topear(monto, entradas);
    }
}
