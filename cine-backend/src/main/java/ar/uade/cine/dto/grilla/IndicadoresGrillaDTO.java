package ar.uade.cine.dto.grilla;

import java.util.Map;

/**
 * {@code ocupacion} viaja calculada para que las pantallas no la dividan cada una a su manera;
 * los minutos van igual porque explican mejor que el porcentaje.
 */
public record IndicadoresGrillaDTO(int minutosProgramados, int minutosDisponibles,
                                   double ocupacion, double puntajePromedio,
                                   int generosCubiertos, int generosTotales,
                                   Map<String, Integer> pasesPorGenero) {
}
