package ar.uade.cine.servicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import ar.uade.cine.dominio.cartelera.EstadoRevision;
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

    /** Para que el mensaje del choque diga una hora y no un LocalDateTime crudo. */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("HH:mm");

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

        // R3: una sala no puede tener dos funciones superpuestas, contando la limpieza.
        LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
        Optional<Funcion> choque = superpuestaEn(salaId, inicio, fin);
        if (choque.isPresent()) {
            throw new IllegalArgumentException(
                    motivoDeLaSuperposicion(choque.get(), salaId, inicio));
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
        // Lo que trajo el importador y nadie miró todavía no se puede dar. Si se pudiera,
        // el buzón de revisión no serviría de nada: bastaría con programar desde ahí para
        // meter en la cartelera del cine algo que nunca nadie aprobó.
        if (pelicula.getEstadoRevision() != EstadoRevision.CONFIRMADA) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " todavía no está confirmada: revisala antes de programarla");
        }
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
     * <p>Lo que ocupa una función no es su duración sino su duración <strong>más la
     * limpieza de la sala</strong>: entre que sale el último espectador y entra el primero
     * de la siguiente hay que levantar la sala, y programar a las 22:05 algo que termina
     * 22:00 es vender una función que empieza con la gente adentro barriendo. El margen se
     * suma a los dos lados —al rango que se consulta y al de cada función ya programada—
     * porque la función nueva también deja la sala sucia para la que venga después.
     *
     * <p>Devuelve cuál y no un boolean porque la grilla tiene que poder decir contra qué
     * choca cada fecha: "el 8 de septiembre ya hay algo a las 20:30" es un informe que se
     * puede leer, "el 8 no se pudo" no.
     */
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        int limpieza = salaDAO.buscarPorId(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        LocalDateTime finConLimpieza = fin.plusMinutes(limpieza);
        for (Funcion existente : funcionDAO.listarPorSala(salaId)) {
            int duracion = peliculaDAO.buscarPorId(existente.getPeliculaId())
                    .map(Pelicula::getDuracionMinutos)
                    .orElse(0);
            LocalDateTime finExistente = existente.getInicio().plusMinutes(duracion).plusMinutes(limpieza);
            if (inicio.isBefore(finExistente) && existente.getInicio().isBefore(finConLimpieza)) {
                return Optional.of(existente);
            }
        }
        return Optional.empty();
    }

    /**
     * Contra qué choca, dicho de manera que se entienda por qué.
     *
     * <p>Existe por un caso puntual: la función anterior termina 22:00, el encargado
     * programa 22:05 y el sistema le dice que la sala está ocupada. Mirando la cartelera
     * no está ocupada —terminó hace cinco minutos— y el mensaje parece un error del
     * sistema. Distinguir el choque real del que produce la limpieza es la diferencia
     * entre "esto está roto" y "ah, corro la función un rato".
     */
    private String motivoDeLaSuperposicion(Funcion choque, int salaId, LocalDateTime inicio) {
        int duracion = peliculaDAO.buscarPorId(choque.getPeliculaId())
                .map(Pelicula::getDuracionMinutos)
                .orElse(0);
        LocalDateTime finReal = choque.getInicio().plusMinutes(duracion);
        if (!inicio.isBefore(finReal)) {
            int limpieza = salaDAO.buscarPorId(salaId).map(Sala::getMinutosLimpieza).orElse(0);
            return "La sala necesita " + limpieza + " minutos de limpieza: la función anterior"
                    + " termina " + finReal.format(MOMENTO) + " y hasta "
                    + finReal.plusMinutes(limpieza).format(MOMENTO) + " no se puede empezar";
        }
        return "La sala ya tiene una función en ese horario";
    }

    public List<Funcion> listar() {
        return funcionDAO.listar();
    }

    /**
     * Las funciones que cumplen los criterios. Cualquier parámetro en {@code null} no
     * filtra, así que {@code buscar(null, null, null, null)} es el listado completo.
     *
     * <p>Es la lista más larga del sistema —una semana de seis salas son más de cien
     * funciones— y por eso es la que más necesita poder acotarse. Los tres criterios son
     * los que usa quien programa: qué película, en qué sala, entre qué fechas.
     *
     * @param desde incluye ese día completo; {@code hasta} también, no es un rango
     *              semiabierto: quien filtra «del 16 al 20» espera ver el 20
     */
    public List<Funcion> buscar(Integer peliculaId, Integer salaId, LocalDate desde, LocalDate hasta) {
        return funcionDAO.listar().stream()
                .filter(f -> peliculaId == null || f.getPeliculaId() == peliculaId)
                .filter(f -> salaId == null || f.getSalaId() == salaId)
                .filter(f -> desde == null || !f.getInicio().toLocalDate().isBefore(desde))
                .filter(f -> hasta == null || !f.getInicio().toLocalDate().isAfter(hasta))
                .toList();
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
