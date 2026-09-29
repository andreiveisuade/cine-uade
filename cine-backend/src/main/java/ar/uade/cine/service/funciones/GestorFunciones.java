package ar.uade.cine.service.funciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

// Alta, baja y consulta de funciones; @Service que aplica lo que la función no ve sola: R3, R12 y R20.
@Service
@Transactional
@RequiredArgsConstructor
public class GestorFunciones {

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("HH:mm");

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final ReservaRepository reservaRepository;
    private final Reloj reloj;

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio,
                             Version version, Proyeccion proyeccion, Dinero precio) {
        return programar(peliculaId, salaId, inicio, version, proyeccion, precio, null);
    }

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, Version version,
                             Proyeccion proyeccion, Dinero precio, Integer programacionId) {
        Pelicula pelicula = peliculaConfirmada(peliculaId);
        Sala sala = salaRepository.exigir(salaId, "la sala");
        Funcion funcion = new Funcion(pelicula, sala, inicio, version, proyeccion, precio, programacionId);
        funcion.exigirProgramableA(reloj.ahora());

        // R3
        LocalDateTime fin = funcion.getFin(pelicula.getDuracionMinutos());
        Optional<Funcion> choque = superpuestaEn(salaId, inicio, fin);
        if (choque.isPresent()) {
            throw new DatoInvalido(motivoDeLaSuperposicion(choque.get(), sala, inicio));
        }
        funcionRepository.save(funcion);
        return funcion;
    }

    // La película que una programación va a repetir, con la misma exigencia que el alta de una suelta.
    // El formato lo valida la programación misma, con las reglas de la función.
    @Transactional(readOnly = true)
    public Pelicula peliculaProgramable(int peliculaId) {
        return peliculaConfirmada(peliculaId);
    }

    // State: la película le pasa la pregunta a su estado de revisión, que rechaza con su texto.
    private Pelicula peliculaConfirmada(int peliculaId) {
        Pelicula pelicula = peliculaRepository.exigir(peliculaId, "la película");
        pelicula.exigirProgramable();
        return pelicula;
    }

    @Transactional(readOnly = true)
    public Optional<Funcion> superpuestaEn(int salaId, LocalDateTime inicio, LocalDateTime fin) {
        return agendaDe(salaId, inicio, fin).chocaCon(inicio, fin);
    }

    private String motivoDeLaSuperposicion(Funcion choque, Sala sala, LocalDateTime inicio) {
        int duracion = peliculaRepository.findById(choque.getPeliculaId())
                .map(Pelicula::getDuracionMinutos)
                .orElse(0);
        LocalDateTime finReal = choque.getFin(duracion);
        if (!inicio.isBefore(finReal)) {
            int limpieza = sala.getMinutosLimpieza();
            return "La sala necesita " + limpieza + " minutos de limpieza: la función anterior"
                    + " termina " + finReal.format(MOMENTO) + " y hasta "
                    + finReal.plusMinutes(limpieza).format(MOMENTO) + " no se puede empezar";
        }
        return "La sala ya tiene una función en ese horario";
    }

    // Solo las funciones que pueden chocar con algo entre desde y hasta, no la historia entera
    // de la sala: una que empezó antes choca si sigue proyectándose o limpiándose, y ninguna
    // dura más que la película más larga del catálogo; una que empieza después choca si
    // arranca antes de que termine la limpieza de lo que se quiere programar.
    @Transactional(readOnly = true)
    public AgendaDeSala agendaDe(int salaId, LocalDateTime desde, LocalDateTime hasta) {
        int limpieza = salaRepository.findById(salaId).map(Sala::getMinutosLimpieza).orElse(0);
        int margen = peliculaRepository.duracionMaxima() + limpieza;
        List<Funcion> funciones = funcionRepository.findBySala_IdAndInicioBetween(salaId,
                desde.minusMinutes(margen), hasta.plusMinutes(limpieza));
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
        return funcionRepository.buscar(peliculaId, salaId,
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<Funcion> listarPorPelicula(int peliculaId) {
        return funcionRepository.findByPelicula_IdOrderByInicioAsc(peliculaId);
    }

    @Transactional(readOnly = true)
    public Optional<Funcion> buscar(int id) {
        return funcionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Funcion obtener(int id) {
        return funcionRepository.exigir(id, "la función");
    }

    public void eliminar(int id) {
        Funcion funcion = funcionRepository.exigir(id, "la función");
        if (reservaRepository.existsByFuncion_Id(id)) {
            throw new DatoInvalido(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.delete(funcion);
    }
}
