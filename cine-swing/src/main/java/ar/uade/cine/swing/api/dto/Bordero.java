package ar.uade.cine.swing.api.dto;

import java.util.Map;

public record Bordero(int funcionId, String pelicula, String sala, String funcion, String generadoEn,
                      int espectadores, double recaudacionBruta, double descuentos, double recaudacionNeta,
                      Map<String, Total> porTarifa) {
}
