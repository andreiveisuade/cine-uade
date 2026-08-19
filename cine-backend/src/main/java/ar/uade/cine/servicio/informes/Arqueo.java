package ar.uade.cine.servicio.informes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Pago;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * El cierre de caja de la boletería de un día: cuánto entró, con cuántas entradas y cómo
 * se repartió entre los medios de pago.
 *
 * <p>Es el resultado de una cuenta del negocio, no una forma de mostrarla: por eso el
 * reparto va con el enum {@link MedioPago} como clave y no con su nombre en texto. Pasar
 * eso a JSON, a un archivo de texto o a lo que venga es problema de quien lo muestre.
 *
 * @param entradas cuántas butacas se vendieron en total, sumando las de cada cobro
 */
public record Arqueo(LocalDate fecha, Dinero total, int entradas,
                     Map<MedioPago, TotalPorMedio> porMedio, List<Pago> pagos) {

    /** Cuántos cobros entraron por un medio y cuánta plata sumaron. */
    public record TotalPorMedio(int cantidad, Dinero total) {
    }
}
