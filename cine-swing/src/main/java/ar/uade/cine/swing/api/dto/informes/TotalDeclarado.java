package ar.uade.cine.swing.api.dto.informes;

import java.util.Map;

// El total del período en la declaración jurada; entradasPorTarifa solo trae las tarifas con venta.
public record TotalDeclarado(int funciones, int espectadores, Map<String, Integer> entradasPorTarifa,
                             double recaudacionBruta, double descuentos, double recaudacionNeta) {
}
