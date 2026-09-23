package ar.uade.cine.swing.api.dto;

import java.util.Map;

public record TotalDeclarado(int funciones, int espectadores, Map<String, Integer> entradasPorTarifa,
                             double recaudacionBruta, double descuentos, double recaudacionNeta) {
}
