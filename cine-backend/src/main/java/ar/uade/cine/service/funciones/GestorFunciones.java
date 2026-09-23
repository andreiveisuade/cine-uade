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

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio,
                             Version version, Proyeccion proyeccion, Dinero precio) {
        return programar(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                             Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        Pelicula pelicula = validarProgramable(peliculaId, salaId, version, proyeccion, precio);
        if (inicio == null) {
            throw new IllegalArgumentException("Falta la fecha y hora de la función");
        }

        // R3
        LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
        Optional<Funcion> choque = superpuestaEn(salaId, inicio, fin);
        if (choque.isPresent()) {
            throw new IllegalArgumentException(
                    motivoDeLaSuperposicion(choque.get(), salaId, inicio));
        }
        // Ya validada arriba: la referencia sale de la sesión, sin otra consulta.
        Funcion funcion = new Funcion(pelicula, salaRepository.getReferenceById(salaId), inicio,
                version, proyeccion, precio, programacionId);
        funcionRepository.save(funcion);
        return funcion;
    }

    /**
     * Lo que no depende del horario. Público para que la grilla lo valide una vez antes de
     * recorrer el rango. Devuelve la película porque de ella sale la duración.
     */
    public Pelicula validarProgramable(int peliculaId, int salaId, Version version,
                                       Proyeccion proyeccion, Dinero precio) {
        Pelicula pelicula = peliculaRepository.findById(peliculaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la película " + peliculaId));
        // Si no, programar saltearía el buzón de revisión.
        if (pelicula.getEstadoRevision() != EstadoRevision.CONFIRMADA) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " todavía no está confirmada: revisala antes de programarla");
        }
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la sala " + salaId));
        if (version == null || proyeccion == null) {
            throw new IllegalArgumentException("Falta la versión o el formato de proyección");
        }
        // R8
        if (proyeccion == Proyeccion.TRES_D && !sala.getTipo().soportaTresD()) {
            throw new IllegalArgumentException("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        return pelicula;
    }

    /**
     * R3. Devuelve cuál y no un boolean para que la grilla diga contra qué choca. La regla
     * de solapamiento vive solo en {@link AgendaDeSala}.
     */
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        return agendaDe(salaId).chocaCon(inicio, fin);
    }

    /** Distingue el choque real del de la limpieza, que si no parece un error del sistema. */
    private String motivoDeLaSuperposicion(Funcion choque, int salaId, LocalDateTime inicio) {
        int duracion = peliculaRepository.findById(choque.getPeliculaId())
                .map(Pelicula::getDuracionMinutos)
                .orElse(0);
        LocalDateTime finReal = choque.getFin(duracion);
        if (!inicio.isBefore(finReal)) {
            int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
            return "La sala necesita " + limpieza + " minutos de limpieza: la función anterior"
                    + " termina " + finReal.format(MOMENTO) + " y hasta "
                    + finReal.plusMinutes(limpieza).format(MOMENTO) + " no se puede empezar";
        }
        return "La sala ya tiene una función en ese horario";
    }

    /**
     * Lo tomado en la sala, en tres consultas: el planificador prueba cientos de horarios.
     * Los tramos ya incluyen la limpieza, para que nadie tenga que acordarse de sumarla.
     */
    public AgendaDeSala agendaDe(int salaId) {
        int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        List<Funcion> funciones = funcionRepository.findBySala_Id(salaId);
        Map<Integer, Integer> duraciones = peliculaRepository
                .findAllById(funciones.stream().map(Funcion::getPeliculaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pelicula::getId, Pelicula::getDuracionMinutos));

        List<AgendaDeSala.Tramo> tomados = funciones.stream()
                .map(f -> new AgendaDeSala.Tramo(f, f.getInicio(),
                        f.getFin(duraciones.getOrDefault(f.getPeliculaId(), 0))
                                .plusMinutes(limpieza)))
                .toList();
        return new AgendaDeSala(limpieza, tomados);
    }

    public List<Funcion> listar() {
        return funcionRepository.findAll();
    }

    /** {@code null} no filtra; {@code desde} y {@code hasta} incluyen el día completo. */
    public List<Funcion> buscar(Integer peliculaId, Integer salaId, LocalDate desde, LocalDate hasta) {
        return funcionRepository.findAll().stream()
                .filter(f -> peliculaId == null || f.getPeliculaId() == peliculaId)
                .filter(f -> salaId == null || f.getSalaId() == salaId)
                .filter(f -> desde == null || !f.getInicio().toLocalDate().isBefore(desde))
                .filter(f -> hasta == null || !f.getInicio().toLocalDate().isAfter(hasta))
                .toList();
    }

    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionRepository.findByPelicula_Id(peliculaId);
    }

    public Optional<Funcion> buscar(int id) {
        return funcionRepository.findById(id);
    }

    /** R12: con reservas, borrarla las dejaría apuntando a la nada. */
    public void eliminar(int id) {
        if (!funcionRepository.existsById(id)) {
            throw new IllegalArgumentException("No existe la función " + id);
        }
        if (reservaRepository.existsByFuncion_Id(id)) {
            throw new IllegalArgumentException(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.deleteById(id);
    }
}
