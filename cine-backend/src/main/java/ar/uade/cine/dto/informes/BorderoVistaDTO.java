package ar.uade.cine.dto.informes;

import java.util.Map;

// El borderó INCAA de una función (GET /api/funciones/{id}/bordero); solo cuenta lo cobrado.
public record BorderoVistaDTO(int funcionId, String pelicula, String sala, String funcion,
                           String generadoEn, int espectadores, double recaudacionBruta,
                           double descuentos, double recaudacionNeta,
                           Map<String, TotalTarifaDTO> porTarifa) {
}
