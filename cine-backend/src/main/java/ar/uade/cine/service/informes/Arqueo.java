package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Cierre de la boletería de un día. Reparto por el enum {@link MedioPago}, sin formatear:
 * mostrarlo es problema de quien lo muestre.
 */
public record Arqueo(LocalDate fecha, Dinero total, int entradas,
                     Map<MedioPago, TotalPorMedio> porMedio, List<Pago> pagos) {

    public record TotalPorMedio(int cantidad, Dinero total) {
    }
}
