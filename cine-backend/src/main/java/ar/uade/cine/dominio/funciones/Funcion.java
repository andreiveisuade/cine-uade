package ar.uade.cine.dominio.funciones;

import java.time.LocalDateTime;

/**
 * Una función programada: una película en una sala, a una fecha y hora, con su versión,
 * formato y precio base. Referencia película y sala por id, no por objeto: quien
 * necesite los datos completos los pide al DAO correspondiente.
 */
public interface Funcion {

    int getId();

    void setId(int id);

    int getPeliculaId();

    int getSalaId();

    /**
     * De qué grilla salió, o {@code null} si la cargó el administrador a mano. Es lo que
     * materializa la asociación con {@link ar.uade.cine.dominio.programaciones.Programacion}.
     *
     * <p>Admite null y no es un {@code int} porque la programación no reemplaza a CU-03:
     * una función suelta —el preestreno del jueves, la función especial— sigue siendo
     * válida y no pertenece a ninguna grilla.
     */
    Integer getProgramacionId();

    LocalDateTime getInicio();

    /** Doblada o subtitulada: es de esta proyección, no de la película. */
    Version getVersion();

    Proyeccion getProyeccion();

    /** Precio base: lo que cuesta una butaca estándar. Los recargos se calculan aparte. */
    double getPrecio();

    // ---------- el paso del tiempo ----------

    /**
     * Si la función ya arrancó. Recibe el instante por parámetro y no lo pide al reloj,
     * por lo mismo que {@code Reserva.estaVencida}: así se puede probar sin esperar.
     *
     * <p>Es lo único de este bloque que no necesita saber cuánto dura la película, y es
     * también lo que sostiene R19: una vez que empezó, no se vende ni se cobra.
     */
    boolean yaEmpezo(LocalDateTime ahora);

    /**
     * Cuándo termina. La duración entra por parámetro porque la función no la conoce:
     * vive en la película, y acá solo hay un {@code peliculaId}. Quien llama ya tuvo que
     * resolver esa relación —es lo mismo que hace {@code GestorFunciones} para validar
     * R3—, así que pedírsela es más honesto que guardar una copia del dato.
     */
    LocalDateTime getFin(int duracionMinutos);

    /** Está proyectándose ahora mismo: ya empezó y todavía no terminó. */
    boolean estaEnCurso(LocalDateTime ahora, int duracionMinutos);

    boolean yaTermino(LocalDateTime ahora, int duracionMinutos);
}
