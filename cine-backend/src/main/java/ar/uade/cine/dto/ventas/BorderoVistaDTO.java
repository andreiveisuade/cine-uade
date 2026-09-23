package ar.uade.cine.dto.ventas;

import java.util.Map;

/**
 * El borderó que se declara al INCAA. Bruta, descuentos y neta viajan separadas para que el
 * front no tenga que explicar la diferencia con datos que no tiene.
 */
public record BorderoVistaDTO(int funcionId, String pelicula, String sala, String funcion,
                           String generadoEn, int espectadores, double recaudacionBruta,
                           double descuentos, double recaudacionNeta,
                           Map<String, TotalTarifaDTO> porTarifa) {
}
