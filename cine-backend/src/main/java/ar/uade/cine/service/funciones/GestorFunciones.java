package ar.uade.cine.service.funciones;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Alta, edición, baja y consulta de funciones; @Service con lo que la función no ve sola: R3, R12 y R20.
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

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, FormatoDeFuncion formato) {
        return programar(peliculaId, salaId, inicio, formato, null);
    }

    public Funcion programar(int peliculaId, int salaId, LocalDateTime inicio, FormatoDeFuncion formato,
                             Integer programacionId) {
        Pelicula pelicula = peliculaConfirmada(peliculaId);
        Sala sala = salaRepository.exigir(salaId, "la sala");
        Funcion funcion = new Funcion(pelicula, sala, inicio, formato, programacionId);
        guardarSiElHorarioEstaLibre(funcion, pelicula, sala);
        return funcion;
    }

    // R12 como el borrado; lo demás, lo mismo que el alta. La de una grilla se desprende (ver Funcion).
    public Funcion reprogramar(int id, int peliculaId, int salaId, LocalDateTime inicio, FormatoDeFuncion formato) {
        Funcion funcion = funcionRepository.exigir(id, "la función");
        if (reservaRepository.existsByFuncion_Id(id)) {
            throw new ConflictoDeNegocio("La función " + id + " tiene reservas: no se puede editar");
        }
        Pelicula pelicula = peliculaConfirmada(peliculaId);
        Sala sala = salaRepository.exigir(salaId, "la sala");
        funcion.reprogramar(pelicula, sala, inicio, formato);
        guardarSiElHorarioEstaLibre(funcion, pelicula, sala);
        return funcion;
    }

    // R20 y R3. La función misma no choca: al editarla, su horario viejo todavía está en la agenda.
    private void guardarSiElHorarioEstaLibre(Funcion funcion, Pelicula pelicula, Sala sala) {
        funcion.exigirProgramableA(reloj.ahora());
        LocalDateTime inicio = funcion.getInicio();
        LocalDateTime fin = funcion.getFin(pelicula.getDuracionMinutos());
        Optional<Funcion> choque = agendaDe(sala.getId(), inicio, fin).sin(funcion).chocaCon(inicio, fin);
        if (choque.isPresent()) {
            throw new DatoInvalido(motivoDeLaSuperposicion(choque.get(), sala, inicio));
        }
        guardarCompitiendoPorElHorario(funcion);
    }

    // Dos pedidos a la vez pasan R3 juntos; el UNIQUE (sala_id, inicio) deja entrar a uno.
    private void guardarCompitiendoPorElHorario(Funcion funcion) {
        try {
            funcionRepository.saveAndFlush(funcion);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoDeNegocio("Ya hay una función en esa sala a esa hora");
        }
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

    // Solo las funciones que pueden chocar entre desde y hasta: las que empezaron antes (ninguna dura más que
    // la película más larga) y las que arrancan antes de que termine la limpieza.
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

    // Una punta del período en null queda abierta; el período ya viene en orden, porque no se arma al revés.
    @Transactional(readOnly = true)
    public List<Funcion> buscar(Integer peliculaId, Integer salaId, Periodo periodo) {
        return funcionRepository.buscar(peliculaId, salaId,
                periodo.desde() == null ? null : periodo.desde().atStartOfDay(),
                periodo.hasta() == null ? null : periodo.hasta().plusDays(1).atStartOfDay());
    }

    // R19: la cartelera ofrece solo lo que todavía se puede comprar.
    @Transactional(readOnly = true)
    public List<Funcion> proximasDe(int peliculaId) {
        LocalDateTime ahora = reloj.ahora();
        return funcionRepository.findByPelicula_IdOrderByInicioAsc(peliculaId).stream()
                .filter(f -> !f.yaEmpezo(ahora))
                .toList();
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
            throw new ConflictoDeNegocio(
                    "La función " + id + " tiene reservas: no se puede eliminar");
        }
        funcionRepository.delete(funcion);
    }
}
