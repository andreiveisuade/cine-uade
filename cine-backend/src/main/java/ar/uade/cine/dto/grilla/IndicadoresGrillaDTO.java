package ar.uade.cine.dto.grilla;

import java.util.Map;

public record IndicadoresGrillaDTO(int minutosProgramados, int minutosDisponibles,
                                   double ocupacion, double puntajePromedio,
                                   int generosCubiertos, int generosTotales,
                                   Map<String, Integer> pasesPorGenero) {
}
