package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.dinero.Dinero;

// Corte de caja de un día, por medio de pago; record de resultado que arma GestorCaja.
public record Arqueo(LocalDate fecha, Dinero total, int entradas,
                     Map<MedioPago, TotalPorMedio> porMedio, List<Pago> pagos) {

    public record TotalPorMedio(int cantidad, Dinero total) {
    }
}
