package ar.uade.cine.dominio.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;

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

    LocalDate getHasta();

    /** La hora a la que arranca cada función de la grilla. */
    LocalTime getHoraInicio();

    /**
     * Vacío significa todos los días, no ninguno. Mismo criterio que
     * {@code Promocion.getDiasSemana()} y que la tabla {@code programacion_dia}: una
     * grilla sin filas de días corre toda la semana.
     */
    Set<DayOfWeek> getDiasSemana();

    /** Las fechas y horas que la grilla quiere ocupar, en orden. */
    List<LocalDateTime> horarios();

    // ---------- lo que se copia a cada función ----------

    Version getVersion();

    Proyeccion getProyeccion();

    double getPrecio();

    /**
     * Una grilla dada de baja no genera funciones nuevas; las ya generadas siguen vivas.
     * Es lo mismo que {@code promocion.activa} y {@code producto.disponible}: en este
     * sistema nada que haya producido ventas se borra.
     */
    boolean estaActiva();

    void setActiva(boolean activa);
}
