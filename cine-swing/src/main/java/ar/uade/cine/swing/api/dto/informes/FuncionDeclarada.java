package ar.uade.cine.swing.api.dto.informes;

import java.util.Map;

// Una función de la declaración jurada con las cifras de su borderó; idioma y proyección por constante.
// `porTarifa` trae solo las tarifas con venta: las demás se completan con 0 al escribir el archivo.
public record FuncionDeclarada(int funcionId, String inicio, String sala, String pelicula, String clasificacion,
                               String idioma, String proyeccion, int espectadores, Map<String, Total> porTarifa,
                               double recaudacionBruta, double descuentos, double recaudacionNeta) {
}
