package ar.uade.cine.service.programaciones;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Grilla automática de varios días; reparte pases al elenco sin pisar R3 ni R20 y mide el tiempo libre.
@Service
@Transactional
@RequiredArgsConstructor
public class PlanificadorGrilla {

    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PropuestaGrilla proponer(CriteriosGrilla criterios) {
        List<Sala> cargadas = salaRepository.findAll();
        if (cargadas.isEmpty()) {
            throw new DatoInvalido("No hay salas cargadas para programar");
        }
        // R8: una grilla en 3D reparte solo entre las salas que lo proyectan.
        List<Sala> salas = cargadas.stream()
                .filter(sala -> criterios.formato().proyeccion().sePuedeProyectarEn(sala.getTipo()))
                .toList();
        if (salas.isEmpty()) {
            throw new DatoInvalido("No hay salas que puedan proyectar en 3D");
        }
        List<Pelicula> elenco = ElencoGrilla.elegir(
                peliculaRepository.findByEstadoRevision(EstadoRevision.CONFIRMADA), criterios.cuantasPeliculas());
        if (elenco.isEmpty()) {
            throw new DatoInvalido(
                    "No hay películas confirmadas para armar la grilla: revisá el buzón de importadas");
        }
        // SRP y Fabricación pura: la ventana horaria y el reparto viven aparte; acá solo se orquesta.
        VentanaDelDia ventana = new VentanaDelDia(criterios, reloj.ahora());
        List<PaseSugerido> pases = new RepartoDePases(elenco, new PuntajeConfiable(elenco))
                .repartir(salas, agendasDe(salas, criterios), ventana);
        return new PropuestaGrilla(elenco, pases,
                IndicadoresGrilla.de(elenco, pases, minutosLibres(salas, ventana, criterios)));
    }

    // Se recalcula en vez de recibirla del cliente, que podría mandar una vieja o adulterada.
    public PropuestaGrilla aplicar(CriteriosGrilla criterios) {
        PropuestaGrilla propuesta = proponer(criterios);
        for (PaseSugerido pase : propuesta.pases()) {
            funciones.programar(pase.peliculaId(), pase.salaId(), pase.inicio(),
                    criterios.formato());
        }
        return propuesta;
    }

    // Una lectura por sala para todo el rango, y no una por intento.
    private Map<Integer, AgendaDeSala> agendasDe(List<Sala> salas, CriteriosGrilla criterios) {
        LocalDateTime desde = criterios.desde().atStartOfDay();
        LocalDateTime hasta = criterios.desde().plusDays(criterios.dias()).atStartOfDay();
        return salas.stream().collect(Collectors.toMap(Sala::getId,
                sala -> funciones.agendaDe(sala.getId(), desde, hasta)));
    }

    private int minutosLibres(List<Sala> salas, VentanaDelDia ventana, CriteriosGrilla criterios) {
        long minutosPorSala = 0;
        for (LocalDate fecha : ventana.fechas()) {
            minutosPorSala += Math.max(Duration.between(ventana.primerIntento(fecha),
                    ventana.cierre(fecha)).toMinutes(), 0);
        }
        int disponibles = (int) (minutosPorSala * salas.size());

        // Solo lo cargado en las salas de la grilla: lo de una sala 2D no le quita tiempo a una en 3D.
        Set<Integer> deLaGrilla = salas.stream().map(Sala::getId).collect(Collectors.toSet());
        List<Funcion> programadas = funciones.buscar(null, null, criterios.periodo()).stream()
                .filter(f -> deLaGrilla.contains(f.getSalaId()))
                // Lo que empezó antes de la ventana de su día (apertura o, hoy, ahora) no la ocupa.
                .filter(f -> !f.getInicio().isBefore(ventana.primerIntento(f.getInicio().toLocalDate())))
                .filter(f -> f.getInicio().isBefore(ventana.cierre(f.getInicio().toLocalDate())))
                .toList();
        Map<Integer, Integer> duraciones = peliculaRepository
                .findAllById(programadas.stream().map(Funcion::getPeliculaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pelicula::getId, Pelicula::getDuracionMinutos));
        int ocupados = programadas.stream()
                .mapToInt(f -> duraciones.getOrDefault(f.getPeliculaId(), 0))
                .sum();

        // Con funciones cargadas a mano el descuento puede pasarse de la ventana.
        return Math.max(disponibles - ocupados, 0);
    }
}
