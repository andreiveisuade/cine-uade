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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.repository.SalaRepository;
import ar.uade.cine.service.programaciones.PropuestaGrilla.IndicadoresGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.model.dinero.Dinero;

@Service
@Transactional
public class PlanificadorGrilla {

    // Crece con la raíz porque TMDB etiqueta de más.
    private static final double BONO_GENERO_NUEVO = 2.0;

    // Votos desde los cuales el puntaje vale solo; con menos, pesa el promedio del catálogo.
    private static final int VOTOS_PARA_CONFIAR = 50;

    private static final int MINUTOS_ENTRE_INTENTOS = 30;

    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;

    public PlanificadorGrilla(PeliculaRepository peliculaRepository, SalaRepository salaRepository, GestorFunciones funciones) {
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.funciones = funciones;
    }

    public PropuestaGrilla proponer(CriteriosGrilla criterios) {
        validar(criterios);
        List<Pelicula> elenco = elegirElenco(criterios.cuantasPeliculas());
        if (elenco.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay películas confirmadas para armar la grilla: revisá el buzón de importadas");
        }
        List<PaseSugerido> pases = repartir(elenco, criterios, promedioDelCatalogo(elenco));
        return new PropuestaGrilla(elenco, pases, medir(elenco, pases, criterios));
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

        double promedio = promedioDelCatalogo(candidatas);
        List<Pelicula> elenco = new ArrayList<>();
        Set<Genero> cubiertos = new HashSet<>();
        while (elenco.size() < cuantas && !candidatas.isEmpty()) {
            Set<Genero> yaCubiertos = Set.copyOf(cubiertos);
            Pelicula mejor = candidatas.stream()
                    .max(Comparator.comparingDouble((Pelicula p) -> valor(p, yaCubiertos, elenco.isEmpty(), promedio))
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
    private double valor(Pelicula pelicula, Set<Genero> cubiertos, boolean primera, double promedio) {
        double puntaje = puntajeConfiable(pelicula, promedio);
        if (primera) {
            return puntaje;
        }
        long nuevos = pelicula.getGeneros().stream().filter(g -> !cubiertos.contains(g)).count();
        return puntaje + BONO_GENERO_NUEVO * Math.sqrt(nuevos);
    }

    // Promedio bayesiano: (v / (v + m)) × nota + (m / (v + m)) × promedio.
    private double puntajeConfiable(Pelicula pelicula, double promedio) {
        int votos = pelicula.getVotos();
        if (votos <= 0) {
            // Con puntaje y sin votos la cargó el encargado a mano: su criterio no se corrige.
            return pelicula.getPuntaje() > 0 ? pelicula.getPuntaje() : promedio;
        }
        double peso = (double) votos / (votos + VOTOS_PARA_CONFIAR);
        return peso * pelicula.getPuntaje() + (1 - peso) * promedio;
    }

    // Ponderado por votos; los ceros de las no votadas hundirían la referencia.
    private double promedioDelCatalogo(List<Pelicula> candidatas) {
        double votos = candidatas.stream().mapToDouble(Pelicula::getVotos).sum();
        if (votos > 0) {
            return candidatas.stream()
                    .mapToDouble(p -> p.getPuntaje() * p.getVotos())
                    .sum() / votos;
        }
        return candidatas.stream()
                .mapToDouble(Pelicula::getPuntaje)
                .filter(p -> p > 0)
                .average()
                .orElse(0);
    }

    private List<PaseSugerido> repartir(List<Pelicula> elenco, CriteriosGrilla criterios,
                                        double promedio) {
        List<Sala> salas = salaRepository.findAll();
        List<PaseSugerido> pases = new ArrayList<>();
        Map<Integer, Integer> asignados = new HashMap<>();
        elenco.forEach(p -> asignados.put(p.getId(), 0));

        Map<Integer, AgendaDeSala> agendas = new HashMap<>();
        salas.forEach(sala -> agendas.put(sala.getId(), funciones.agendaDe(sala.getId())));

        for (int dia = 0; dia < criterios.dias(); dia++) {
            LocalDate fecha = criterios.desde().plusDays(dia);
            for (Sala sala : salas) {
                LocalDateTime momento = fecha.atTime(criterios.apertura());
                LocalDateTime limite = fecha.atTime(criterios.cierreEfectivo());

                while (momento.isBefore(limite)) {
                    Pelicula elegida = conMasDeuda(elenco, asignados, promedio);
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

    private Pelicula conMasDeuda(List<Pelicula> elenco, Map<Integer, Integer> asignados,
                                 double promedio) {
        return elenco.stream()
                .min(Comparator.comparingDouble(
                                (Pelicula p) -> asignados.get(p.getId()) / peso(p, promedio))
                        .thenComparing(Pelicula::getTitulo))
                .orElseThrow();
    }

    // Nunca cero: con puntaje 0 la deuda sería infinita y se llevaría la grilla entera.
    private double peso(Pelicula pelicula, double promedio) {
        return Math.max(puntajeConfiable(pelicula, promedio), 0.1);
    }

    private int minutosLibres(CriteriosGrilla criterios) {
        long minutosPorDia = Duration.between(criterios.apertura(), criterios.cierreEfectivo()).toMinutes();
        int ventana = (int) (minutosPorDia * criterios.dias() * salaRepository.count());

        LocalDate hasta = criterios.desde().plusDays(criterios.dias() - 1L);
        List<Funcion> programadas = funciones.buscar(null, null, criterios.desde(), hasta).stream()
                .filter(f -> !f.getInicio().toLocalTime().isBefore(criterios.apertura()))
                .filter(f -> f.getInicio().toLocalTime().isBefore(criterios.cierreEfectivo()))
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

    private IndicadoresGrilla medir(List<Pelicula> elenco, List<PaseSugerido> pases,
                                    CriteriosGrilla criterios) {
        int programados = pases.stream().mapToInt(PaseSugerido::duracionMinutos).sum();
        int disponibles = minutosLibres(criterios);

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

    private void validar(CriteriosGrilla criterios) {
        if (criterios.desde() == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio de la grilla");
        }
        if (criterios.dias() <= 0) {
            throw new IllegalArgumentException("La grilla tiene que cubrir al menos un día");
        }
        if (criterios.cuantasPeliculas() <= 0) {
            throw new IllegalArgumentException("Hay que programar al menos una película");
        }
        if (criterios.precio() == null || !criterios.precio().esMayorQue(Dinero.CERO)) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (!criterios.apertura().isBefore(criterios.cierreEfectivo())) {
            throw new IllegalArgumentException("El cine tiene que cerrar después de abrir");
        }
        if (salaRepository.count() == 0) {
            throw new IllegalArgumentException("No hay salas cargadas para programar");
        }
    }
}
