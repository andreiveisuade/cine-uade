package ar.uade.cine.service.funciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Necesita película y sala porque la regla R3 no se puede validar solo con funciones:
 * cuánto dura cada una sale de la película que proyecta.
 */
@Service
@Transactional
public class GestorFunciones {

    /** Para que el mensaje del choque diga una hora y no un LocalDateTime crudo. */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("HH:mm");

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;

    public GestorFunciones(FuncionRepository funcionRepository, PeliculaRepository peliculaRepository, SalaRepository salaRepository,
                           ReservaRepository reservaRepository) {
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.reservaRepository = reservaRepository;
    }

    /** Devuelve la función ya con su id, igual que GestorCartelera.agregar. */
    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio,
                             Version version, Proyeccion proyeccion, Dinero precio) {
        return programar(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    /**
     * La misma alta, dejando escrito de qué grilla salió. La usa
     * {@link GestorProgramaciones} al materializar una programación; con
     * {@code programacionId} en null es la función suelta que carga el administrador.
     */
    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                             Proyeccion proyeccion, Dinero precio, Integer programacionId) {
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
        Funcion funcion = new Funcion(peliculaId, salaId, inicio, version, proyeccion, precio,
                programacionId);
        funcionRepository.save(funcion);
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
                                       Proyeccion proyeccion, Dinero precio) {
        Pelicula pelicula = peliculaRepository.findById(peliculaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + peliculaId));
        // Lo que trajo el importador y nadie miró todavía no se puede dar. Si se pudiera,
        // el buzón de revisión no serviría de nada: bastaría con programar desde ahí para
        // meter en la cartelera del cine algo que nunca nadie aprobó.
        if (pelicula.getEstadoRevision() != EstadoRevision.CONFIRMADA) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " todavía no está confirmada: revisala antes de programarla");
        }
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        if (version == null || proyeccion == null) {
            throw new IllegalArgumentException("Falta la versión o el formato de proyección");
        }
        // R8: no programar 3D en una sala que no lo soporta.
        if (proyeccion == Proyeccion.TRES_D && !sala.getTipo().soportaTresD()) {
            throw new IllegalArgumentException("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
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
        int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        LocalDateTime finConLimpieza = fin.plusMinutes(limpieza);
        for (Funcion existente : funcionRepository.findBySalaId(salaId)) {
            int duracion = peliculaRepository.findById(existente.getPeliculaId())
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
        int duracion = peliculaRepository.findById(choque.getPeliculaId())
                .map(Pelicula::getDuracionMinutos)
                .orElse(0);
        LocalDateTime finReal = choque.getInicio().plusMinutes(duracion);
        if (!inicio.isBefore(finReal)) {
            int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
            return "La sala necesita " + limpieza + " minutos de limpieza: la función anterior"
                    + " termina " + finReal.format(MOMENTO) + " y hasta "
                    + finReal.plusMinutes(limpieza).format(MOMENTO) + " no se puede empezar";
        }
        return "La sala ya tiene una función en ese horario";
    }

    /**
     * La misma regla R3 que {@link #superpuestaEn}, pero con las lecturas hechas una sola
     * vez, para quien tenga que preguntar muchas veces por la misma sala.
     *
     * <p>{@code superpuestaEn} relee las funciones de la sala y la duración de cada
     * película en cada llamada. Está bien para un alta suelta y es carísimo para el
     * planificador, que prueba cientos de horarios por corrida: así una propuesta de una
     * semana tardaba más de veinte segundos contra MySQL, casi todo en repetir las mismas
     * consultas.
     *
     * <p>Devolver la agenda armada y no una lista cruda es lo que mantiene la regla acá:
     * quien la use compara contra tramos que ya incluyen la limpieza, sin saber cómo se
     * calculó ni tener que acordarse de sumarla.
     */
    public AgendaDeSala agendaDe(int salaId) {
        int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        Map<Integer, Integer> duraciones = peliculaRepository.findAll().stream()
                .collect(Collectors.toMap(Pelicula::getId, Pelicula::getDuracionMinutos));

        List<AgendaDeSala.Tramo> tomados = funcionRepository.findBySalaId(salaId).stream()
                .map(f -> new AgendaDeSala.Tramo(f.getInicio(),
                        f.getInicio().plusMinutes(duraciones.getOrDefault(f.getPeliculaId(), 0))
                                .plusMinutes(limpieza)))
                .toList();
        return new AgendaDeSala(limpieza, tomados);
    }

    public List<Funcion> listar() {
        return funcionRepository.findAll();
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
        return funcionRepository.findAll().stream()
                .filter(f -> peliculaId == null || f.getPeliculaId() == peliculaId)
                .filter(f -> salaId == null || f.getSalaId() == salaId)
                .filter(f -> desde == null || !f.getInicio().toLocalDate().isBefore(desde))
                .filter(f -> hasta == null || !f.getInicio().toLocalDate().isAfter(hasta))
                .toList();
    }

    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionRepository.findByPeliculaId(peliculaId);
    }

    public Optional<Funcion> buscar(int id) {
        return funcionRepository.findById(id);
    }

    /** R12: si tiene entradas vendidas, borrarla dejaría reservas apuntando a la nada. */
    public void eliminar(int id) {
        if (funcionRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("No existe la función " + id);
        }
        if (!reservaRepository.findByFuncionId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.deleteById(id);
    }
}
