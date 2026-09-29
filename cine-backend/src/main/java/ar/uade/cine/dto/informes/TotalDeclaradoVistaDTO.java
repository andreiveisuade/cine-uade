package ar.uade.cine.dto.informes;

import java.util.Map;

// El total del período en la declaración jurada; entradasPorTarifa solo trae las tarifas con venta.
public record TotalDeclaradoVistaDTO(int funciones, int espectadores, Map<String, Integer> entradasPorTarifa,
                                     double recaudacionBruta, double descuentos, double recaudacionNeta) {
}
