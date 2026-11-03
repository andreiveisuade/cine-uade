package ar.uade.cine.swing.api.dto.programaciones;

import java.util.Map;

// Las medidas de una propuesta del planificador: minutos, ocupación, puntaje, géneros y facturación esperada.
public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles, double ocupacion,
                                double puntajePromedio, int generosCubiertos, int generosTotales,
                                Map<String, Integer> pasesPorGenero, double facturacionEsperada) {
}
