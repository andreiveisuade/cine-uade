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
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.ReservaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.model.dinero.Dinero;

/**
 * Las funciones programadas y sus reglas: R3 (no se pisan en la sala) y R8 (3D solo
 * donde se puede). Necesita película y sala porque la duración sale de la película.
 */
@Service
@Transactional
public class GestorFunciones {

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

    /** La función suelta que carga el administrador: no salió de ninguna grilla. */
    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio,
                             Version version, Proyeccion proyeccion, Dinero precio) {
        return programar(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    /** La misma alta, dejando escrito de qué grilla salió. */
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
     * Lo que tiene que valer sin mirar el horario. Devuelve la película porque de ella
     * sale la duración. Es público para que la grilla lo valide una vez antes de recorrer
     * el rango: si la sala no proyecta en 3D, no hay ninguna fecha en la que sí.
     */
    public Pelicula validarProgramable(int peliculaId, int salaId, Version version,
                                       Proyeccion proyeccion, Dinero precio) {
        Pelicula pelicula = peliculaRepository.findById(peliculaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + peliculaId));
        // Sin esto el buzón no serviría: bastaría programar para dar algo que nadie aprobó.
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
     * R3: la función de esa sala que se pisa con ese rango, si hay alguna. Devuelve cuál
     * y no un boolean para que la grilla pueda decir contra qué choca cada fecha.
     *
     * <p>Es {@link #agendaDe} preguntada una vez: la regla de solapamiento —con la
     * limpieza sumada a los dos lados— vive en la agenda y en ningún otro lado.
     */
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        return agendaDe(salaId).chocaCon(inicio, fin);
    }

    /**
     * Distingue el choque real del que produce la limpieza: la anterior termina 22:00, el
     * encargado programa 22:05 y "sala ocupada" parece un error del sistema.
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
     * Lo que la sala ya tiene tomado, leído de una vez: sus funciones y la duración de
     * cada película, en tres consultas. El planificador prueba cientos de horarios por
     * corrida y con una lectura por intento una propuesta tardaba más de veinte segundos.
     *
     * <p>Devuelve la agenda armada y no la lista cruda: los tramos ya incluyen la
     * limpieza, así que quien pregunta no tiene que acordarse de sumarla.
     */
    public AgendaDeSala agendaDe(int salaId) {
        int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        List<Funcion> funciones = funcionRepository.findBySalaId(salaId);
        Map<Integer, Integer> duraciones = peliculaRepository
                .findAllById(funciones.stream().map(Funcion::getPeliculaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pelicula::getId, Pelicula::getDuracionMinutos));

        List<AgendaDeSala.Tramo> tomados = funciones.stream()
                .map(f -> new AgendaDeSala.Tramo(f, f.getInicio(),
                        f.getInicio().plusMinutes(duraciones.getOrDefault(f.getPeliculaId(), 0))
                                .plusMinutes(limpieza)))
                .toList();
        return new AgendaDeSala(limpieza, tomados);
    }

    public List<Funcion> listar() {
        return funcionRepository.findAll();
    }

    /**
     * Las funciones que cumplen los criterios; {@code null} no filtra.
     *
     * @param desde incluye ese día completo; {@code hasta} también: quien filtra «del 16
     *              al 20» espera ver el 20
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
        if (!funcionRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe la función " + id);
        }
        if (reservaRepository.existsByFuncionId(id)) {
            throw new IllegalArgumentException(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.deleteById(id);
    }
}
