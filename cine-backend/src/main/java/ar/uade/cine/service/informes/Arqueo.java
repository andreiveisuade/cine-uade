package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.dinero.Dinero;

// Corte de caja de un día, por medio de pago; Experto en sumar los pagos del día que le pasa GestorCaja.
public record Arqueo(LocalDate fecha, Dinero total, int entradas,
                     Map<MedioPago, TotalPorMedio> porMedio, List<Pago> pagos) {

    // Las reservas son las de esos pagos: una reserva tiene a lo sumo un pago, así que cada una
    // cuenta sus entradas una sola vez.
    public static Arqueo de(LocalDate fecha, List<Pago> pagos, List<Reserva> reservas) {
        Map<MedioPago, TotalPorMedio> porMedio = new EnumMap<>(MedioPago.class);
        Dinero total = Dinero.CERO;
        for (Pago pago : pagos) {
            porMedio.merge(pago.getMedio(), new TotalPorMedio(1, pago.getMonto()), TotalPorMedio::mas);
            total = total.mas(pago.getMonto());
        }
        int entradas = reservas.stream().mapToInt(Reserva::getCantidadEntradas).sum();
        return new Arqueo(fecha, total, entradas, porMedio, pagos);
    }

    public record TotalPorMedio(int cantidad, Dinero total) {

        TotalPorMedio mas(TotalPorMedio otro) {
            return new TotalPorMedio(cantidad + otro.cantidad, total.mas(otro.total));
        }
    }
}
