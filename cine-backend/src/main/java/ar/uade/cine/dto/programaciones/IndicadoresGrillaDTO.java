package ar.uade.cine.dto.programaciones;

import java.util.Map;

// Las medidas de la grilla automática (/api/grilla): minutos, ocupación, puntaje y géneros cubiertos.
public record IndicadoresGrillaDTO(int minutosProgramados, int minutosDisponibles,
                                   double ocupacion, double puntajePromedio,
                                   int generosCubiertos, int generosTotales,
                                   Map<String, Integer> pasesPorGenero) {
}
