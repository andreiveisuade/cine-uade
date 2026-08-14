package ar.uade.cine.dto.ventas;

import java.util.Map;

/**
 * El borderó de una función: lo que se declara al INCAA. La clave de {@code porTarifa} es
 * el nombre del enum, igual que la de {@code porMedio} en el arqueo.
 *
 * <p>Las tres cifras viajan separadas y no resumidas en una: {@code recaudacionBruta} es a
 * precio de lista, {@code descuentos} lo que resignó el cine por una promoción y
 * {@code recaudacionNeta} lo que entró en la caja. Mandar solo la última obligaría al front
 * a explicar una diferencia con datos que no tiene.
 */
public record BorderoVistaDTO(int funcionId, String pelicula, String sala, String funcion,
                           String generadoEn, int espectadores, double recaudacionBruta,
                           double descuentos, double recaudacionNeta,
                           Map<String, TotalTarifaDTO> porTarifa) {
}
