package ar.uade.cine.dominio.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * La grilla con la que un cine define su cartelera: "Matrix en la Sala 1, todos los días
 * a las 20:30, del 1 al 15 de septiembre". Una sola alta en vez de quince.
 *
 * <p><strong>Genera funciones de verdad, no las calcula al vuelo.</strong> Es la decisión
 * que define la entidad. Una función tiene cosas propias que la grilla no sabe ni puede
 * saber: sus reservas, si se canceló, si se movió de sala porque el proyector se rompió.
 * Derivarlas de la grilla en cada consulta obligaría a modelar cada una de esas
 * excepciones como una excepción <em>a la grilla</em>, y a la tercera el modelo sería la
 * grilla más una lista de parches. Materializarlas deja a la función siendo lo que ya era.
 *
 * <p>Lleva encima todo lo que necesita una {@code Funcion} para nacer —película, sala,
 * versión, proyección, precio— más el patrón temporal que las multiplica: rango de fechas,
 * hora y días de la semana. Las referencias van por id, como en todo el dominio.
 */
public interface Programacion {

    int getId();

    void setId(int id);

    int getPeliculaId();

    int getSalaId();

    // ---------- el patrón temporal ----------

    LocalDate getDesde();

    /**
     * Cuándo termina, o {@code null} si la grilla es <strong>abierta</strong>: corre hasta
     * que alguien la dé de baja. Es lo normal en un cine —la función de las 20:30 no tiene
     * fecha de vencimiento— y es lo que le da sentido a {@link #estaActiva()}: sin grillas
     * abiertas, dar de baja no evita nada, porque no quedaba nada por generar.
     */
    LocalDate getHasta();

    /** La hora a la que arranca cada función de la grilla. */
    LocalTime getHoraInicio();

    /**
     * Vacío significa todos los días, no ninguno. Mismo criterio que
     * {@code Promocion.getDiasSemana()} y que la tabla {@code programacion_dia}: una
     * grilla sin filas de días corre toda la semana.
     */
    Set<DayOfWeek> getDiasSemana();

    /**
     * Hasta qué fecha ya se materializaron las funciones, o {@code null} si todavía
     * ninguna.
     *
     * <p>Es lo único de la grilla que no describe la intención sino lo que efectivamente
     * pasó, y se guarda en vez de derivarse a propósito: mirar la última función generada
     * no sirve, porque una función se puede cancelar o mover de sala y entonces la cuenta
     * daría de menos y se volverían a generar las mismas fechas.
     */
    LocalDate getGeneradaHasta();

    void setGeneradaHasta(LocalDate generadaHasta);

    /**
     * Las fechas y horas que la grilla quiere ocupar, en orden, sin pasarse del
     * {@code tope}.
     *
     * <p>El tope entra por parámetro y no lo decide la grilla porque una grilla abierta no
     * tiene final: quien pregunta es el que sabe hasta dónde le interesa materializar. Una
     * grilla cerrada nunca se pasa de su propio {@code hasta}, aunque el tope sea posterior.
     */
    List<LocalDateTime> horarios(LocalDate tope);

    // ---------- lo que se copia a cada función ----------

    Version getVersion();

    Proyeccion getProyeccion();

    Dinero getPrecio();

    /**
     * Una grilla dada de baja no genera funciones nuevas; las ya generadas siguen vivas.
     * Es lo mismo que {@code promocion.activa} y {@code producto.disponible}: en este
     * sistema nada que haya producido ventas se borra.
     */
    boolean estaActiva();

    void setActiva(boolean activa);
}
