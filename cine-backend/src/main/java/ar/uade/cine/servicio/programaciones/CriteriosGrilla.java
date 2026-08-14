package ar.uade.cine.servicio.programaciones;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Con qué restricciones se arma una grilla semanal.
 *
 * <p>Va como record y no como nueve parámetros sueltos por lo que le pasó a
 * {@code GestorFunciones.buscar}: una firma de nueve argumentos del mismo tipo se llama
 * mal una vez y no lo nota nadie —{@code apertura} y {@code cierre} son los dos
 * {@code LocalTime}— y además esto viaja entero desde la pantalla que lo configura.
 *
 * @param desde             primer día de la grilla
 * @param dias              cuántos días cubre. Una semana es lo normal
 * @param apertura          a partir de qué hora se puede programar la primera función
 * @param cierre            hasta qué hora tiene que haber terminado la última. No es
 *                          cuándo puede empezar: una película de tres horas no entra a las
 *                          23:00 aunque el cine cierre a medianoche
 * @param cuantasPeliculas  cuántos títulos distintos entran en la semana. Es el número que
 *                          convierte «tengo dieciocho importadas» en «doy ocho»
 * @param precio            el precio base de cada función que se genere
 */
public record CriteriosGrilla(LocalDate desde, int dias, LocalTime apertura, LocalTime cierre,
                              int cuantasPeliculas, Dinero precio, Version version,
                              Proyeccion proyeccion) {

    /** Lo habitual: una semana, de 14 a 24, ocho títulos. */
    public static CriteriosGrilla deUnaSemana(LocalDate desde, int cuantasPeliculas, Dinero precio) {
        return new CriteriosGrilla(desde, 7, LocalTime.of(14, 0), LocalTime.of(0, 0),
                cuantasPeliculas, precio, Version.SUBTITULADA, Proyeccion.DOS_D);
    }

    /**
     * El cierre pasada la medianoche se escribe {@code 00:00} y hay que leerlo como el
     * final del día, no como su principio. Sin esto, la última función posible sería la de
     * las 23:59 del día anterior a la que arranca la ventana.
     */
    public LocalTime cierreEfectivo() {
        return cierre.equals(LocalTime.MIDNIGHT) ? LocalTime.of(23, 59) : cierre;
    }
}
