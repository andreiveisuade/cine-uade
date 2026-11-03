package ar.uade.cine.service.programaciones;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.ventas.Entrada;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Grilla automática de varios días; llena todas las salas sin pisar R3 ni R20 y mide ocupación y facturación.
@Service
@Transactional
@RequiredArgsConstructor
public class PlanificadorGrilla {

    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final GestorSalas salas;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PropuestaGrilla proponer(CriteriosGrilla criterios) {
        List<Sala> todas = salaRepository.findAll();
        if (todas.isEmpty()) {
            throw new DatoInvalido("No hay salas cargadas para programar");
        }
        List<Pelicula> elenco = ElencoGrilla.elegir(
                peliculaRepository.findByEstadoRevision(EstadoRevision.CONFIRMADA), criterios.cuantasPeliculas());
        if (elenco.isEmpty()) {
            throw new DatoInvalido(
                    "No hay películas confirmadas para armar la grilla: revisá el buzón de importadas");
        }
        // SRP y Fabricación pura: la ventana horaria y el reparto viven aparte; acá solo se orquesta.
        VentanaDelDia ventana = new VentanaDelDia(criterios, reloj.ahora());
        PuntajeConfiable puntajes = new PuntajeConfiable(elenco);
        List<PaseSugerido> pases = new RepartoDePases(elenco, puntajes)
                .repartir(todas, agendasDe(todas, criterios), ventana);
        return new PropuestaGrilla(elenco, pases, IndicadoresGrilla.de(elenco, pases,
                minutosLibres(todas, ventana, criterios), aSalaLlena(todas, criterios.precio()), puntajes));
    }

    // Se recalcula en vez de recibirla del cliente, que podría mandar una vieja o adulterada.
    public PropuestaGrilla aplicar(CriteriosGrilla criterios) {
        PropuestaGrilla propuesta = proponer(criterios);
        for (PaseSugerido pase : propuesta.pases()) {
            funciones.programar(pase.peliculaId(), pase.salaId(), pase.inicio(),
                    criterios.formatoEn(pase.proyeccion()));
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

        List<Funcion> programadas = funciones.buscar(null, null, criterios.periodo()).stream()
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

    // Lo que recauda un pase a sala llena con la tarifa general, la misma cuenta que la entrada; sin las rotas (R9).
    private Function<PaseSugerido, Dinero> aSalaLlena(List<Sala> todas, Dinero precio) {
        Map<Integer, Sala> porId = todas.stream().collect(Collectors.toMap(Sala::getId, sala -> sala));
        Map<Integer, List<Asiento>> asientos = salas.asientosDe(porId.keySet());
        return pase -> {
            Sala sala = porId.get(pase.salaId());
            return asientos.getOrDefault(sala.getId(), List.of()).stream()
                    .filter(asiento -> !asiento.estaFueraDeServicio())
                    .map(asiento -> Entrada.precioDe(TipoTarifa.GENERAL,
                            asiento.getTipo().aplicarA(sala.getTipo().aplicarA(precio)), pase.proyeccion()))
                    .reduce(Dinero.CERO, Dinero::mas);
        };
    }
}
