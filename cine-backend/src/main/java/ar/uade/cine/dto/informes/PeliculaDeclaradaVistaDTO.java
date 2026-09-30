package ar.uade.cine.dto.informes;

import java.util.Map;

// Una película de la declaración jurada: sus funciones sumadas; entradasPorTarifa solo trae las vendidas.
public record PeliculaDeclaradaVistaDTO(String titulo, String clasificacion, int funciones,
                                        int espectadores, Map<String, Integer> entradasPorTarifa,
                                        double recaudacionBruta, double descuentos,
                                        double recaudacionNeta) {
}
