package ar.uade.cine.dto.grilla;

import java.util.Map;

/**
 * Los números con los que se juzga una grilla propuesta.
 *
 * <p>{@code ocupacion} viaja calculada y no como dos minutos sueltos para que las dos
 * pantallas que la muestran no la dividan cada una a su manera. Los minutos van igual
 * porque «7 de 10 horas» explica mejor que «70%» por qué queda lugar.
 */
public record IndicadoresGrillaDTO(int minutosProgramados, int minutosDisponibles,
                                   double ocupacion, double puntajePromedio,
                                   int generosCubiertos, int generosTotales,
                                   Map<String, Integer> pasesPorGenero) {
}
