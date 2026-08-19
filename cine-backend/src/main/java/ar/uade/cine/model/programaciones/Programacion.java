package ar.uade.cine.model.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * La grilla con la que un cine define su cartelera: "Matrix en la Sala 1, todos los días
 * a las 20:30, del 1 al 15 de septiembre". Una sola alta en vez de quince.
 *
 * <p><strong>Genera funciones de verdad, no las calcula al vuelo.</strong> Es la decisión
 * que define la entidad. Una función tiene cosas propias que la grilla no sabe ni puede
 * saber: sus reservas, si se canceló, si se movió de sala porque el proyector se rompió.
 * Derivarlas de la grilla en cada consulta obligaría a modelar cada una de esas excepciones
 * como una excepción <em>a la grilla</em>, y a la tercera el modelo sería la grilla más una
 * lista de parches. Materializarlas deja a la función siendo lo que ya era.
 *
 * <p>Lleva encima todo lo que necesita una {@code Funcion} para nacer —película, sala,
 * versión, proyección, precio— más el patrón temporal que las multiplica: rango de fechas,
 * hora y días de la semana. Las referencias van por id, como en todo el dominio.
 */
@Entity
public class Programacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "pelicula_id")
    private int peliculaId;

    @Column(name = "sala_id")
    private int salaId;

    // ---------- el patrón temporal ----------

    private LocalDate desde;

    /**
     * Cuándo termina, o {@code null} si la grilla es <strong>abierta</strong>: corre hasta
     * que alguien la dé de baja. Es lo normal en un cine —la función de las 20:30 no tiene
     * fecha de vencimiento— y es lo que le da sentido a {@link #estaActiva()}: sin grillas
     * abiertas, dar de baja no evita nada, porque no quedaba nada por generar.
     */
    private LocalDate hasta;

    private LocalTime horaInicio;

    /**
     * Vacío significa todos los días, no ninguno. Mismo criterio que
     * {@code Promocion.getDiasSemana()}: una grilla sin filas de días corre toda la semana.
     *
     * <p>EAGER porque sin los días la grilla no se puede leer: cualquier cosa que se haga
     * con ella —listarla, generar funciones— pregunta qué días corre.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programacion_dia",
            joinColumns = @JoinColumn(name = "programacion_id"))
    @Column(name = "dia")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);

    // ---------- lo que se copia a cada función ----------

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    private Dinero precio;

    private boolean activa = true;

    private LocalDate generadaHasta;

    protected Programacion() {
    }

    public Programacion(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                        LocalTime horaInicio, Set<DayOfWeek> diasSemana, Version version,
                        Proyeccion proyeccion, Dinero precio) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.desde = desde;
        this.hasta = hasta;
        this.horaInicio = horaInicio;
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasSemana);
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
    }

    /**
     * Recorre el rango día por día y se queda con los que la grilla habilita. Vive acá y no
     * en el gestor porque no consulta nada ni decide nada del negocio: es cómo se lee el
     * patrón temporal que la propia programación guarda.
     *
     * <p>El final es el más cercano entre el {@code hasta} de la grilla y el tope que pide
     * quien llama. Una grilla abierta no tiene el primero, así que manda el tope; una
     * cerrada no se pasa del suyo ni aunque el tope sea posterior.
     */
    public List<LocalDateTime> horarios(LocalDate tope) {
        LocalDate fin = hasta == null || tope.isBefore(hasta) ? tope : hasta;
        List<LocalDateTime> momentos = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (diasSemana.isEmpty() || diasSemana.contains(dia.getDayOfWeek())) {
                momentos.add(LocalDateTime.of(dia, horaInicio));
            }
        }
        return momentos;
    }

    /**
     * Hasta qué fecha ya se materializaron las funciones, o {@code null} si todavía ninguna.
     *
     * <p>Es lo único de la grilla que no describe la intención sino lo que efectivamente
     * pasó, y se guarda en vez de derivarse a propósito: mirar la última función generada no
     * sirve, porque una función se puede cancelar o mover de sala y entonces la cuenta daría
     * de menos y se volverían a generar las mismas fechas.
     */
    public LocalDate getGeneradaHasta() {
        return generadaHasta;
    }

    public void setGeneradaHasta(LocalDate generadaHasta) {
        this.generadaHasta = generadaHasta;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeliculaId() {
        return peliculaId;
    }

    public int getSalaId() {
        return salaId;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    /** La hora a la que arranca cada función de la grilla. */
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public Set<DayOfWeek> getDiasSemana() {
        return diasSemana;
    }

    public Version getVersion() {
        return version;
    }

    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    public Dinero getPrecio() {
        return precio;
    }

    /**
     * Una grilla dada de baja no genera funciones nuevas; las ya generadas siguen vivas. Es
     * lo mismo que {@code promocion.activa} y {@code producto.disponible}: en este sistema
     * nada que haya producido ventas se borra.
     */
    public boolean estaActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + horaInicio
                + " del " + desde + (hasta == null ? " en adelante" : " al " + hasta)
                + " - generada hasta " + (generadaHasta == null ? "nunca" : generadaHasta);
    }
}
