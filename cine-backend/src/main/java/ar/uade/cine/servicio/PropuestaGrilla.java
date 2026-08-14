package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;

/**
 * Qué grilla propone el planificador, y con qué números se la puede defender.
 *
 * <p>Trae las tres cosas juntas a propósito: <em>a quién eligió</em> (el elenco),
 * <em>qué hace con ellos</em> (los pases) y <em>qué tan buena salió</em> (los
 * indicadores). Una propuesta sin indicadores obliga a confiar; con ellos se puede
 * comparar contra otra —seis títulos o diez, abrir a las 14 o a las 16— y elegir con un
 * criterio en vez de con una corazonada.
 *
 * <p>Es el resultado de una cuenta del negocio y no una forma de mostrarla, mismo criterio
 * que {@link Arqueo} y {@link PlanProgramacion}: los momentos van como
 * {@code LocalDateTime} y los géneros como el enum, no formateados.
 */
public record PropuestaGrilla(List<Pelicula> elenco, List<PaseSugerido> pases,
                              IndicadoresGrilla indicadores) {

    /** Una función propuesta: qué película, en qué sala y cuándo. Todavía no existe. */
    public record PaseSugerido(int peliculaId, String titulo, int salaId, String sala,
                               LocalDateTime inicio, int duracionMinutos) {
    }

    /**
     * Los números con los que se juzga la grilla. Son tres preguntas distintas y por eso
     * son tres familias de campos: cuánto se aprovechan las salas, qué tan buena es la
     * cartelera y qué tan variada.
     *
     * @param minutosProgramados  suma de la duración de todos los pases
     * @param minutosDisponibles  el tiempo de sala que la propuesta podía usar: la ventana
     *                            entera <strong>menos</strong> lo que ya estaba programado.
     *                            Descontarlo es lo que hace que la ocupación signifique lo
     *                            que dice — con el denominador entero, una semana con las
     *                            salas llenas daba un número bajo que se leía como cine
     *                            vacío, cuando era al revés
     * @param puntajePromedio     promedio ponderado por pases: una película que va cuatro
     *                            veces pesa cuatro veces. Es lo que ve el espectador
     *                            promedio que entra sin saber qué va a ver
     * @param generosCubiertos    cuántos géneros distintos toca el elenco
     * @param generosTotales      cuántos existen, para poder leer el anterior como fracción
     * @param pasesPorGenero      cuántas funciones toca cada género. Es lo que delata una
     *                            grilla que en el papel es diversa pero da acción todo el
     *                            día y un documental a las tres de la tarde
     */
    public record IndicadoresGrilla(int minutosProgramados, int minutosDisponibles,
                                    double puntajePromedio, int generosCubiertos,
                                    int generosTotales, Map<Genero, Integer> pasesPorGenero) {

        /** Qué porcentaje del tiempo de sala queda vendido, de 0 a 1. */
        public double ocupacion() {
            return minutosDisponibles == 0 ? 0 : (double) minutosProgramados / minutosDisponibles;
        }
    }
}
