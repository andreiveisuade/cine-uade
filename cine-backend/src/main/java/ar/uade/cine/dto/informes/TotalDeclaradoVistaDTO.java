package ar.uade.cine.dto.informes;

import java.util.Map;

public record TotalDeclaradoVistaDTO(int funciones, int espectadores, Map<String, Integer> entradasPorTarifa,
                                     double recaudacionBruta, double descuentos, double recaudacionNeta) {
}
