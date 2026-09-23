package ar.uade.cine.dto.ventas;

import java.util.Map;

public record BorderoVistaDTO(int funcionId, String pelicula, String sala, String funcion,
                           String generadoEn, int espectadores, double recaudacionBruta,
                           double descuentos, double recaudacionNeta,
                           Map<String, TotalTarifaDTO> porTarifa) {
}
