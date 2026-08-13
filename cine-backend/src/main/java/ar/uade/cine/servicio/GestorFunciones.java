package ar.uade.cine.servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;

/**
 * Necesita película y sala porque la regla R3 no se puede validar solo con funciones:
 * cuánto dura cada una sale de la película que proyecta.
 */
public class GestorFunciones {

    private final FuncionDAO funcionDAO;
    private final PeliculaDAO peliculaDAO;
    private final SalaDAO salaDAO;
    private final ReservaDAO reservaDAO;

    public GestorFunciones(FuncionDAO funcionDAO, PeliculaDAO peliculaDAO, SalaDAO salaDAO,
                           ReservaDAO reservaDAO) {
        this.funcionDAO = funcionDAO;
        this.peliculaDAO = peliculaDAO;
        this.salaDAO = salaDAO;
        this.reservaDAO = reservaDAO;
    }

    /** Devuelve la función ya con su id, igual que GestorCartelera.agregar. */
    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio,
                             Version version, Proyeccion proyeccion, double precio) {
        return programar(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    /**
     * La misma alta, dejando escrito de qué grilla salió. La usa
     * {@link GestorProgramaciones} al materializar una programación; con
     * {@code programacionId} en null es la función suelta que carga el administrador.
     */
    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                             Proyeccion proyeccion, double precio, Integer programacionId) {
        Pelicula pelicula = validarProgramable(peliculaId, salaId, version, proyeccion, precio);
        if (inicio == null) {
            throw new IllegalArgumentException("Falta la fecha y hora de la función");
        }

        // R3: una sala no puede tener dos funciones superpuestas.
        LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
        if (superpuestaEn(salaId, inicio, fin).isPresent()) {
            throw new IllegalArgumentException("La sala ya tiene una función en ese horario");
        }
        Funcion funcion = new FuncionImpl(peliculaId, salaId, inicio, version, proyeccion, precio,
                programacionId);
        funcionDAO.guardar(funcion);
        return funcion;
    }

    /**
     * Todo lo que tiene que valer sin mirar el horario, y la película que se va a
     * proyectar —que es de donde sale la duración, y por eso la devuelve en vez de
     * limitarse a un boolean.
     *
     * <p>Es público porque la grilla necesita validar estas mismas condiciones
     * <strong>una vez</strong>, antes de recorrer el rango: si la sala no proyecta en 3D,
     * no hay ninguna fecha del mes en la que sí. Sin esto, previsualizar una grilla
     * imposible mostraría quince funciones perfectas y recién explotaría al confirmar.
     */
    public Pelicula validarProgramable(int peliculaId, int salaId, Version version,
                                       Proyeccion proyeccion, double precio) {
        Pelicula pelicula = peliculaDAO.buscarPorId(peliculaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + peliculaId));
        Sala sala = salaDAO.buscarPorId(salaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        if (version == null || proyeccion == null) {
            throw new IllegalArgumentException("Falta la versión o el formato de proyección");
        }
        // R8: no programar 3D en una sala que no lo soporta.
        if (proyeccion == Proyeccion.TRES_D && !sala.getTipo().soportaTresD()) {
            throw new IllegalArgumentException("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        return pelicula;
    }

    /**
     * R3: la función de esa sala que se pisa con ese rango, si hay alguna. Dos rangos se
     * pisan si cada uno empieza antes de que termine el otro.
     *
     * <p>Devuelve cuál y no un boolean porque la grilla tiene que poder decir contra qué
     * choca cada fecha: "el 8 de septiembre ya hay algo a las 20:30" es un informe que se
     * puede leer, "el 8 no se pudo" no.
     */
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        for (Funcion existente : funcionDAO.listarPorSala(salaId)) {
            int duracion = peliculaDAO.buscarPorId(existente.getPeliculaId())
                    .map(Pelicula::getDuracionMinutos)
                    .orElse(0);
            LocalDateTime finExistente = existente.getInicio().plusMinutes(duracion);
            if (inicio.isBefore(finExistente) && existente.getInicio().isBefore(fin)) {
                return Optional.of(existente);
            }
        }
        return Optional.empty();
    }

    public List<Funcion> listar() {
        return funcionDAO.listar();
    }

    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionDAO.listarPorPelicula(peliculaId);
    }

    public Optional<Funcion> buscar(int id) {
        return funcionDAO.buscarPorId(id);
    }

    /** R12: si tiene entradas vendidas, borrarla dejaría reservas apuntando a la nada. */
    public void eliminar(int id) {
        if (funcionDAO.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la función " + id);
        }
        if (!reservaDAO.listarPorFuncion(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionDAO.eliminar(id);
    }
}
