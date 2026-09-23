package ar.uade.cine.dto.ventas;

import java.util.Map;

public record PeliculaDeclaradaVistaDTO(String titulo, String clasificacion, int funciones,
                                        int espectadores, Map<String, Integer> entradasPorTarifa,
                                        double recaudacionBruta, double descuentos,
                                        double recaudacionNeta) {
}
