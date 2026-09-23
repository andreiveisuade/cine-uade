package ar.uade.cine.swing.api.dto;

import java.util.Map;

public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles, double ocupacion,
                                double puntajePromedio, int generosCubiertos, int generosTotales,
                                Map<String, Integer> pasesPorGenero) {
}
