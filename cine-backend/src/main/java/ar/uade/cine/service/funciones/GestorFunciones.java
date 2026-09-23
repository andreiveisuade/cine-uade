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
import ar.uade.cine.service.RecursoNoEncontrado;

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

    @Transactional(readOnly = true)
    public Pelicula validarProgramable(int peliculaId, int salaId, Version version,
                                       Proyeccion proyeccion, Dinero precio) {
        Pelicula pelicula = peliculaRepository.findById(peliculaId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la película " + peliculaId));
        // Si no, programar saltearía el buzón de revisión.
        if (pelicula.getEstadoRevision() != EstadoRevision.CONFIRMADA) {
            throw new IllegalArgumentException("La película " + pelicula.getTitulo()
                    + " todavía no está confirmada: revisala antes de programarla");
        }
        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la sala " + salaId));
        if (version == null || proyeccion == null) {
            throw new IllegalArgumentException("Falta la versión o el formato de proyección");
        }
        if (proyeccion == Proyeccion.TRES_D && !sala.getTipo().soportaTresD()) {
            throw new IllegalArgumentException("La sala " + sala.getNombre() + " no puede proyectar en 3D");
        }
        if (precio == null || !precio.esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        return pelicula;
    }

    @Transactional(readOnly = true)
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        return agendaDe(salaId).chocaCon(inicio, fin);
    }

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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<Funcion> listar() {
        return funcionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Funcion> buscar(Integer peliculaId, Integer salaId, LocalDate desde, LocalDate hasta) {
        return funcionRepository.findAll().stream()
                .filter(f -> peliculaId == null || f.getPeliculaId() == peliculaId)
                .filter(f -> salaId == null || f.getSalaId() == salaId)
                .filter(f -> desde == null || !f.getInicio().toLocalDate().isBefore(desde))
                .filter(f -> hasta == null || !f.getInicio().toLocalDate().isAfter(hasta))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionRepository.findByPelicula_Id(peliculaId);
    }

    @Transactional(readOnly = true)
    public Optional<Funcion> buscar(int id) {
        return funcionRepository.findById(id);
    }

    public void eliminar(int id) {
        if (!funcionRepository.existsById(id)) {
            throw new RecursoNoEncontrado("No existe la función " + id);
        }
        if (reservaRepository.existsByFuncion_Id(id)) {
            throw new IllegalArgumentException(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.deleteById(id);
    }
}
