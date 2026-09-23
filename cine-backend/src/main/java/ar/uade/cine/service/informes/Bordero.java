package ar.uade.cine.service.informes;

import java.time.LocalDateTime;
import java.util.Map;

import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Lo que el cine declara al INCAA por función. No se deriva del {@link Arqueo}: el arqueo
 * corta por día y las entradas de una función se venden a lo largo de varios. Bruta,
 * descuentos y neta van separadas para explicar por qué la caja no da entradas × precio.
 *
 * @param porTarifa el desglose que pide el organismo, por {@link TipoTarifa}
 * @param generadoEn lo sella el gestor, que conoce el reloj; el generador solo da formato
 */
public record Bordero(int funcionId, String pelicula, String sala, LocalDateTime funcion,
                      LocalDateTime generadoEn, int espectadores,
                      Dinero recaudacionBruta, Dinero descuentos, Dinero recaudacionNeta,
                      Map<TipoTarifa, TotalPorTarifa> porTarifa) {

    /** A precio de lista. */
    public record TotalPorTarifa(int cantidad, Dinero total) {
    }
}
