package ar.uade.cine.service.programaciones;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.programaciones.PropuestaGrilla.IndicadoresGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Grilla automática de varios días; elige elenco, reparte pases y los mide, con R3 de GestorFunciones y R20.
@Service
@Transactional
@RequiredArgsConstructor
public class PlanificadorGrilla {

    // Crece con la raíz porque TMDB etiqueta de más.
    private static final double BONO_GENERO_NUEVO = 2.0;

    private static final int MINUTOS_ENTRE_INTENTOS = 30;

    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PropuestaGrilla proponer(CriteriosGrilla criterios) {
        List<Sala> salas = salaRepository.findAll();
        if (salas.isEmpty()) {
            throw new DatoInvalido("No hay salas cargadas para programar");
        }
        List<Pelicula> elenco = elegirElenco(criterios.cuantasPeliculas());
        if (elenco.isEmpty()) {
            throw new DatoInvalido(
                    "No hay películas confirmadas para armar la grilla: revisá el buzón de importadas");
        }
        List<PaseSugerido> pases = repartir(elenco, salas, criterios, new PuntajeConfiable(elenco));
        return new PropuestaGrilla(elenco, pases, medir(elenco, pases, salas, criterios));
    }

    // Se recalcula en vez de recibirla del cliente, que podría mandar una vieja o adulterada.
    public PropuestaGrilla aplicar(CriteriosGrilla criterios) {
        PropuestaGrilla propuesta = proponer(criterios);
        for (PaseSugerido pase : propuesta.pases()) {
            funciones.programar(pase.peliculaId(), pase.salaId(), pase.inicio(),
                    criterios.version(), criterios.proyeccion(), criterios.precio());
        }
        return propuesta;
    }

    private List<Pelicula> elegirElenco(int cuantas) {
        List<Pelicula> candidatas = new ArrayList<>(
                peliculaRepository.findByEstadoRevision(EstadoRevision.CONFIRMADA).stream()
                        .filter(p -> p.getDuracionMinutos() > 0)
                        .toList());

        PuntajeConfiable puntajes = new PuntajeConfiable(candidatas);
        List<Pelicula> elenco = new ArrayList<>();
        Set<Genero> cubiertos = new HashSet<>();
        while (elenco.size() < cuantas && !candidatas.isEmpty()) {
            Set<Genero> yaCubiertos = Set.copyOf(cubiertos);
            Pelicula mejor = candidatas.stream()
                    .max(Comparator.comparingDouble((Pelicula p) -> valor(p, yaCubiertos, elenco.isEmpty(), puntajes))
                            // Desempate por título, para que la propuesta sea reproducible.
                            .thenComparing(Pelicula::getTitulo, Comparator.reverseOrder()))
                    .orElseThrow();
            elenco.add(mejor);
            cubiertos.addAll(mejor.getGeneros());
            candidatas.remove(mejor);
        }
        return elenco;
    }

    // La primera no lleva bono: premiaría a la que tiene más etiquetas de TMDB.
    private double valor(Pelicula pelicula, Set<Genero> cubiertos, boolean primera,
                         PuntajeConfiable puntajes) {
        double puntaje = puntajes.de(pelicula);
        if (primera) {
            return puntaje;
        }
        long nuevos = pelicula.getGeneros().stream().filter(g -> !cubiertos.contains(g)).count();
        return puntaje + BONO_GENERO_NUEVO * Math.sqrt(nuevos);
    }

    private List<PaseSugerido> repartir(List<Pelicula> elenco, List<Sala> salas, CriteriosGrilla criterios,
                                        PuntajeConfiable puntajes) {
        List<PaseSugerido> pases = new ArrayList<>();
        Map<Integer, Integer> asignados = new HashMap<>();
        elenco.forEach(p -> asignados.put(p.getId(), 0));

        Map<Integer, AgendaDeSala> agendas = new HashMap<>();
        LocalDateTime desde = criterios.desde().atStartOfDay();
        LocalDateTime hasta = criterios.desde().plusDays(criterios.dias()).atStartOfDay();
        salas.forEach(sala -> agendas.put(sala.getId(), funciones.agendaDe(sala.getId(), desde, hasta)));

        for (int dia = 0; dia < criterios.dias(); dia++) {
            LocalDate fecha = criterios.desde().plusDays(dia);
            for (Sala sala : salas) {
                LocalDateTime momento = primerIntento(fecha, criterios);
                LocalDateTime limite = criterios.cierreDe(fecha);

                while (momento.isBefore(limite)) {
                    Pelicula elegida = conMasDeuda(elenco, asignados, puntajes);
                    LocalDateTime fin = momento.plusMinutes(elegida.getDuracionMinutos());
                    if (fin.isAfter(limite)) {
                        // No se prueba una más corta: se llevaría siempre el último turno.
                        break;
                    }
                    if (agendas.get(sala.getId()).chocaEn(momento, fin)) {
                        momento = momento.plusMinutes(MINUTOS_ENTRE_INTENTOS);
                        continue;
                    }
                    pases.add(new PaseSugerido(elegida.getId(), elegida.getTitulo(),
                            sala.getId(), sala.getNombre(), momento, elegida.getDuracionMinutos()));
                    asignados.merge(elegida.getId(), 1, Integer::sum);
                    momento = fin.plusMinutes(sala.getMinutosLimpieza());
                }
            }
        }
        return pases;
    }

    // R20: hoy arranca en el primer intento que todavía no pasó; en un día que ya pasó da el
    // cierre o más, sin nada por repartir. Lo usan el reparto y la ventana de los indicadores,
    // para que la ocupación se mida contra lo que el reparto puede usar de verdad.
    private LocalDateTime primerIntento(LocalDate fecha, CriteriosGrilla criterios) {
        LocalDateTime momento = fecha.atTime(criterios.apertura());
        LocalDateTime limite = criterios.cierreDe(fecha);
        while (momento.isBefore(limite) && Funcion.yaPaso(momento, reloj.ahora())) {
            momento = momento.plusMinutes(MINUTOS_ENTRE_INTENTOS);
        }
        return momento;
    }

    private Pelicula conMasDeuda(List<Pelicula> elenco, Map<Integer, Integer> asignados,
                                 PuntajeConfiable puntajes) {
        return elenco.stream()
                .min(Comparator.comparingDouble(
                                (Pelicula p) -> asignados.get(p.getId()) / peso(p, puntajes))
                        .thenComparing(Pelicula::getTitulo))
                .orElseThrow();
    }

    // Nunca cero: con puntaje 0 la deuda sería infinita y se llevaría la grilla entera.
    private double peso(Pelicula pelicula, PuntajeConfiable puntajes) {
        return Math.max(puntajes.de(pelicula), 0.1);
    }

    private int minutosLibres(List<Sala> salas, CriteriosGrilla criterios) {
        long minutosPorSala = 0;
        for (int dia = 0; dia < criterios.dias(); dia++) {
            LocalDate fecha = criterios.desde().plusDays(dia);
            minutosPorSala += Math.max(Duration.between(primerIntento(fecha, criterios),
                    criterios.cierreDe(fecha)).toMinutes(), 0);
        }
        int ventana = (int) (minutosPorSala * salas.size());

        List<Funcion> programadas = funciones.buscar(null, null, criterios.periodo()).stream()
                // Lo que empezó antes de la ventana de su día (apertura o, hoy, ahora) no la ocupa.
                .filter(f -> !f.getInicio().isBefore(primerIntento(f.getInicio().toLocalDate(), criterios)))
                .filter(f -> f.getInicio().isBefore(criterios.cierreDe(f.getInicio().toLocalDate())))
                .toList();
        Map<Integer, Integer> duraciones = peliculaRepository
                .findAllById(programadas.stream().map(Funcion::getPeliculaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pelicula::getId, Pelicula::getDuracionMinutos));
        int ocupados = programadas.stream()
                .mapToInt(f -> duraciones.getOrDefault(f.getPeliculaId(), 0))
                .sum();

        // Con funciones cargadas a mano el descuento puede pasarse de la ventana.
        return Math.max(ventana - ocupados, 0);
    }

    private IndicadoresGrilla medir(List<Pelicula> elenco, List<PaseSugerido> pases, List<Sala> salas,
                                    CriteriosGrilla criterios) {
        int programados = pases.stream().mapToInt(PaseSugerido::duracionMinutos).sum();
        int disponibles = minutosLibres(salas, criterios);

        Map<Integer, Pelicula> porId = new HashMap<>();
        elenco.forEach(p -> porId.put(p.getId(), p));
        double puntajeTotal = pases.stream()
                .mapToDouble(pase -> porId.get(pase.peliculaId()).getPuntaje())
                .sum();

        Map<Genero, Integer> porGenero = new EnumMap<>(Genero.class);
        for (PaseSugerido pase : pases) {
            for (Genero genero : porId.get(pase.peliculaId()).getGeneros()) {
                porGenero.merge(genero, 1, Integer::sum);
            }
        }
        Set<Genero> cubiertos = new LinkedHashSet<>();
        elenco.forEach(p -> cubiertos.addAll(p.getGeneros()));

        return new IndicadoresGrilla(programados, disponibles,
                pases.isEmpty() ? 0 : puntajeTotal / pases.size(),
                cubiertos.size(), Genero.values().length, porGenero);
    }
}
