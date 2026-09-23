package ar.uade.cine.service.programaciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;

/**
 * La grilla propuesta (elenco y pases) con los indicadores que permiten compararla contra
 * otra en vez de confiar. Datos del negocio sin formatear, como {@link PlanProgramacion}.
 */
public record PropuestaGrilla(List<Pelicula> elenco, List<PaseSugerido> pases,
                              IndicadoresGrilla indicadores) {

    /** Una función propuesta, todavía no creada. */
    public record PaseSugerido(int peliculaId, String titulo, int salaId, String sala,
                               LocalDateTime inicio, int duracionMinutos) {
    }

    /**
     * @param minutosDisponibles la ventana <strong>menos</strong> lo ya programado; si no,
     *                           una semana llena mostraría ocupación baja
     * @param puntajePromedio    ponderado por pases: una película que va cuatro veces pesa cuatro
     * @param pasesPorGenero     delata una grilla diversa en el papel que da acción todo el día
     */
    public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles,
                                    double puntajePromedio, int generosCubiertos,
                                    int generosTotales, Map<Genero, Integer> pasesPorGenero) {

        /** De 0 a 1. */
        public double ocupacion() {
            return minutosDisponibles == 0 ? 0 : (double) minutosProgramados / minutosDisponibles;
        }
    }
}
