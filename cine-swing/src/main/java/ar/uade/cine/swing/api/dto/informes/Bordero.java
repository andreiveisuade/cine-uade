package ar.uade.cine.swing.api.dto.informes;

import java.util.Map;

// El borderó INCAA de una función como lo arma el backend; Swing lo muestra y lo guarda como texto.
public record Bordero(int funcionId, String pelicula, String sala, String funcion, String generadoEn,
                      int espectadores, double recaudacionBruta, double descuentos, double recaudacionNeta,
                      Map<String, Total> porTarifa) {
}
