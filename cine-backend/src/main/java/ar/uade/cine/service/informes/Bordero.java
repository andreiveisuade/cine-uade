package ar.uade.cine.service.informes;

import java.time.LocalDateTime;
import java.util.Map;

import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

public record Bordero(int funcionId, String pelicula, String sala, LocalDateTime funcion,
                      LocalDateTime generadoEn, int espectadores,
                      Dinero recaudacionBruta, Dinero descuentos, Dinero recaudacionNeta,
                      Map<TipoTarifa, TotalPorTarifa> porTarifa) {

    public record TotalPorTarifa(int cantidad, Dinero total) {
    }
}
