package ar.uade.cine.dominio.funciones;

import java.time.LocalDateTime;

/**
 * Referencia a película y sala por id: el DAO trae el objeto completo cuando hace falta.
 */
public class FuncionImpl implements Funcion {

    private int id;
    private final int peliculaId;
    private final int salaId;
    private final Integer programacionId;
    private final LocalDateTime inicio;
    private final Version version;
    private final Proyeccion proyeccion;
    private final double precio;

    /** La función suelta de CU-03: no salió de ninguna grilla. */
    public FuncionImpl(int peliculaId, int salaId, LocalDateTime inicio,
                       Version version, Proyeccion proyeccion, double precio) {
        this(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    public FuncionImpl(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                       Proyeccion proyeccion, double precio, Integer programacionId) {
        this.peliculaId = peliculaId;
        this.salaId = salaId;
        this.inicio = inicio;
        this.version = version;
        this.proyeccion = proyeccion;
        this.precio = precio;
        this.programacionId = programacionId;
    }

    public FuncionImpl(int id, int peliculaId, int salaId, LocalDateTime inicio, Version version,
                       Proyeccion proyeccion, double precio, Integer programacionId) {
        this(peliculaId, salaId, inicio, version, proyeccion, precio, programacionId);
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getPeliculaId() {
        return peliculaId;
    }

    @Override
    public int getSalaId() {
        return salaId;
    }

    @Override
    public Integer getProgramacionId() {
        return programacionId;
    }

    @Override
    public LocalDateTime getInicio() {
        return inicio;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Proyeccion getProyeccion() {
        return proyeccion;
    }

    @Override
    public double getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return "[" + id + "] película " + peliculaId + " en sala " + salaId + " - " + inicio
                + " - " + proyeccion + " " + version + " - desde $" + precio;
    }

    // ---------- el paso del tiempo ----------

    @Override
    public boolean yaEmpezo(LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    @Override
    public LocalDateTime getFin(int duracionMinutos) {
        return inicio.plusMinutes(duracionMinutos);
    }

    @Override
    public boolean estaEnCurso(LocalDateTime ahora, int duracionMinutos) {
        return yaEmpezo(ahora) && !yaTermino(ahora, duracionMinutos);
    }

    @Override
    public boolean yaTermino(LocalDateTime ahora, int duracionMinutos) {
        return getFin(duracionMinutos).isBefore(ahora);
    }
}
