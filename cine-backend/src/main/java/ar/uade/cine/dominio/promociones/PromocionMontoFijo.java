package ar.uade.cine.dominio.promociones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.ventas.Entrada;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.dinero.Dinero;

/** Una cantidad fija de plata: el "$2000 off pagando con tal banco". */
public class PromocionMontoFijo extends PromocionBase {

    private final Dinero monto;

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
