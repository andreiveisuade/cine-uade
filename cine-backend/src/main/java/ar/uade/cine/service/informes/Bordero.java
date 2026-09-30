package ar.uade.cine.service.informes;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.Pago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

// Borderó de una función: espectadores y recaudación por tarifa; Experto en sumar lo cobrado de sus reservas.
public record Bordero(int funcionId, String pelicula, String sala, LocalDateTime funcion,
                      LocalDateTime generadoEn, int espectadores,
                      Dinero recaudacionBruta, Dinero descuentos, Dinero recaudacionNeta,
                      Map<TipoTarifa, TotalPorTarifa> porTarifa) {

    // Se declara lo cobrado: una reserva sin pagar retiene butacas pero no vendió.
    public static Bordero de(Funcion funcion, LocalDateTime generadoEn, List<Reserva> reservas,
                             Map<Integer, Pago> pagosPorReserva) {
        Map<TipoTarifa, TotalPorTarifa> porTarifa = new EnumMap<>(TipoTarifa.class);
        int espectadores = 0;
        Dinero bruta = Dinero.CERO;
        Dinero descuentos = Dinero.CERO;
        Dinero neta = Dinero.CERO;

        for (Reserva reserva : reservas) {
            Pago pago = pagosPorReserva.get(reserva.getId());
            if (pago == null) {
                continue;
            }
            for (Entrada entrada : reserva.getEntradas()) {
                porTarifa.merge(entrada.tarifa(), new TotalPorTarifa(1, entrada.precio()), TotalPorTarifa::mas);
                espectadores++;
            }
            // Desglose a precio de lista; totales con el pago, único que sabe cuánto sacó la promo.
            bruta = bruta.mas(pago.getSubtotal());
            descuentos = descuentos.mas(pago.getDescuento());
            neta = neta.mas(pago.getMonto());
        }

        return new Bordero(funcion.getId(), funcion.getPelicula().getTitulo(), funcion.getSala().getNombre(),
                funcion.getInicio(), generadoEn, espectadores, bruta, descuentos, neta, porTarifa);
    }

    public record TotalPorTarifa(int cantidad, Dinero total) {

        TotalPorTarifa mas(TotalPorTarifa otro) {
            return new TotalPorTarifa(cantidad + otro.cantidad, total.mas(otro.total));
        }
    }
}
