package ar.uade.cine.model.funciones;

import java.time.LocalDateTime;

import ar.uade.cine.model.dinero.Dinero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Una función programada: una película en una sala, a una fecha y hora, con su versión,
 * formato y precio base.
 *
 * <p>Referencia película y sala <strong>por id</strong> y no con un {@code @ManyToOne}:
 * quien necesite los datos completos se los pide al repositorio. No es una traducción
 * incompleta, es la misma decisión de siempre dicha en el mapeo. Listar las funciones de
 * una semana son cien filas, y con la referencia al objeto cada una arrastraría su película
 * y su sala aunque la pantalla solo pinte el horario. Las relaciones sí se mapean donde una
 * cosa no existe sin la otra —las entradas de una reserva, los géneros de una película—,
 * que es lo que distingue una parte de un vecino.
 */
@Entity
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "pelicula_id")
    private int peliculaId;

    @Column(name = "sala_id")
    private int salaId;

    /**
     * De qué grilla salió, o {@code null} si la cargó el administrador a mano. Es lo que
     * materializa la asociación con {@link ar.uade.cine.model.programaciones.Programacion}.
     *
     * <p>Admite null y es {@code Integer} y no {@code int} porque la programación no
     * reemplaza a CU-03: una función suelta —el preestreno del jueves, la función especial—
     * sigue siendo válida y no pertenece a ninguna grilla.
     */
    @Column(name = "programacion_id")
    private Integer programacionId;

    private LocalDateTime inicio;

    @Enumerated(EnumType.STRING)
    private Version version;

    @Enumerated(EnumType.STRING)
    private Proyeccion proyeccion;

    /** Precio base: lo que cuesta una butaca estándar. Los recargos se calculan aparte. */
    private Dinero precio;

    protected Funcion() {
    }

    /** La función suelta de CU-03: no salió de ninguna grilla. */
    public Funcion(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio) {
        this(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    public Funcion(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                   Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.inicio = inicio;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        this.programacionId = programacionId;
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

    public Integer getProgramacionId() {
        return programacionId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    /** Doblada o subtitulada: es de esta proyección, no de la película. */
    public Version getVersion() {
        return version;
    }

    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    public Dinero getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + inicio
                + " - " + proyeccion + " " + version + " - desde $" + precio;
    }

    // ---------- el paso del tiempo ----------

    /**
     * Si la función ya arrancó. Recibe el instante por parámetro y no lo pide al reloj, por
     * lo mismo que {@code Reserva.estaVencida}: así se puede probar sin esperar.
     *
     * <p>Es lo único de este bloque que no necesita saber cuánto dura la película, y es
     * también lo que sostiene R19: una vez que empezó, no se vende ni se cobra.
     */
    public boolean yaEmpezo(LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    /**
     * Cuándo termina. La duración entra por parámetro porque la función no la conoce: vive
     * en la película, y acá solo hay un {@code peliculaId}. Quien llama ya tuvo que resolver
     * esa relación —es lo mismo que hace {@code GestorFunciones} para validar R3—, así que
     * pedírsela es más honesto que guardar una copia del dato.
     */
    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }

    /** Está proyectándose ahora mismo: ya empezó y todavía no terminó. */
    public boolean estaEnCurso(LocalDateTime ahora, int duracionMinutos) {
        return yaEmpezo(ahora) && !yaTermino(ahora, duracionMinutos);
    }

    public boolean yaTermino(LocalDateTime ahora, int duracionMinutos) {
        return getFin(duracionMinutos).isBefore(ahora);
    }
}
