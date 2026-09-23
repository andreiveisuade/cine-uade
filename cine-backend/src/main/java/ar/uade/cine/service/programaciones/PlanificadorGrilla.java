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

/**
 * Arma la grilla de la semana: elige qué películas se dan y las reparte en las salas.
 *
 * <p>Optimiza tres cosas a la vez porque ninguna sola alcanza: <strong>puntaje</strong>
 * (solo con eso la grilla es monotemática), <strong>diversidad</strong> de géneros (solo
 * con eso entra cualquier cosa) y <strong>ocupación</strong> de sala (solo con eso gana la
 * película más corta).
 *
 * <p>No reescribe R3: le pregunta a {@link GestorFunciones#agendaDe} y respeta las
 * funciones ya cargadas. Es determinista —mismos criterios, misma propuesta—, que es lo
 * que permite previsualizar y aplicar como dos llamadas sin que el resultado cambie.
 */
@Service
@Transactional
public class PlanificadorGrilla {

    /**
     * Cuánto vale que una película traiga géneros que el elenco todavía no cubre. Dos
     * puntos sobre diez alcanza para que una comedia de 7,0 le gane a la cuarta de acción
     * de 8,5. El bono crece con la raíz de los géneros nuevos, no linealmente: TMDB
     * etiqueta con generosidad y una película de cuatro géneros no aporta cuatro veces
     * más variedad.
     */
    private static final double BONO_GENERO_NUEVO = 2.0;

    /**
     * Votos a partir de los cuales el puntaje vale por sí solo. En la cartelera real la
     * mediana es treinta y cuatro y siete de veinte títulos no tienen ninguno: con
     * cincuenta, la mitad del catálogo queda entre su nota y el promedio general.
     */
    private static final int VOTOS_PARA_CONFIAR = 50;

    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;

    public PlanificadorGrilla(PeliculaRepository peliculaRepository, SalaRepository salaRepository, GestorFunciones funciones) {
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
        this.funciones = funciones;
    }

    /** La propuesta, sin escribir nada. */
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

    /**
     * La misma propuesta, creando las funciones. Se recalcula en vez de recibirla hecha:
     * si el cliente mandara la propuesta de vuelta, podría mandar una distinta o una
     * vieja, y el alta escribiría algo que ninguna regla revisó.
     */
    public PropuestaGrilla aplicar(CriteriosGrilla criterios) {
        PropuestaGrilla propuesta = proponer(criterios);
        for (PaseSugerido pase : propuesta.pases()) {
            funciones.programar(pase.peliculaId(), pase.salaId(), pase.inicio(),
                    criterios.version(), criterios.proyeccion(), criterios.precio());
        }
        return propuesta;
    }

    // ---------- etapa 1: quiénes ----------

    /**
     * Elige el elenco con un goloso: cada vuelta toma la de mayor valor —su puntaje más un
     * bono por los géneros que todavía nadie cubre— y al elegirla esos géneros dejan de
     * sumar. Goloso y no el óptimo exacto porque la diferencia es de decimales y el
     * resultado tiene que poder explicarse en una pantalla.
     */
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

    /**
     * Cuánto vale esta película con el elenco a medio armar. La primera no lleva bono: el
     * bono mide cuánta variedad <em>agrega</em>, y con el elenco vacío no hay a qué
     * agregarle; aplicarlo igual premiaría a la que tiene más etiquetas de TMDB.
     */
    private double valor(Pelicula pelicula, Set<Genero> cubiertos, boolean primera, double promedio) {
        double puntaje = puntajeConfiable(pelicula, promedio);
        if (primera) {
            return puntaje;
        }
        long nuevos = pelicula.getGeneros().stream().filter(g -> !cubiertos.contains(g)).count();
        return puntaje + BONO_GENERO_NUEVO * Math.sqrt(nuevos);
    }

    /**
     * El puntaje corregido por cuánta gente lo votó: un 8,0 sobre seis votos no es la
     * misma información que sobre cinco mil, y un 0,0 sin votos es una película que nadie
     * vio, no una mala. Es la corrección que usan IMDb y TMDB: la nota pesa más cuantos
     * más votos tiene, y el resto lo pone el promedio del catálogo.
     *
     * <pre>  valor = (v / (v + m)) × nota  +  (m / (v + m)) × promedio</pre>
     */
    private double puntajeConfiable(Pelicula pelicula, double promedio) {
        int votos = pelicula.getVotos();
        if (votos <= 0) {
            // Con puntaje y sin votos es la que cargó el encargado a mano: ese número es su
            // criterio, no se corrige. En cero y sin votos es la de TMDB que nadie vio: "no sé".
            return pelicula.getPuntaje() > 0 ? pelicula.getPuntaje() : promedio;
        }
        double peso = (double) votos / (votos + VOTOS_PARA_CONFIAR);
        return peso * pelicula.getPuntaje() + (1 - peso) * promedio;
    }

    /**
     * La nota promedio del catálogo, ponderada por votos: una con seis votos no puede mover
     * la referencia tanto como una con cinco mil. Los ceros de las no votadas no entran,
     * porque hundirían la referencia contra la que se las corrige.
     */
    private double promedioDelCatalogo(List<Pelicula> candidatas) {
        double votos = candidatas.stream().mapToDouble(Pelicula::getVotos).sum();
        if (votos > 0) {
            return candidatas.stream()
                    .mapToDouble(p -> p.getPuntaje() * p.getVotos())
                    .sum() / votos;
        }
        // Sin ninguna votada, el promedio de las que tienen puntaje es mejor referencia que cero.
        return candidatas.stream()
                .mapToDouble(Pelicula::getPuntaje)
                .filter(p -> p > 0)
                .average()
                .orElse(0);
    }

    // ---------- etapa 2: dónde y cuándo ----------

    /**
     * Llena cada sala, día por día, desde la apertura hasta que no entre una función más.
     * En cada hueco entra la película con más «deuda» —menos pases en relación a su
     * puntaje—, así la mejor termina con cuatro o cinco funciones diarias y la octava con
     * una. Si el hueco ya está tomado (R3, preguntado a la agenda) se corre al siguiente.
     *
     * <p>La agenda se lee una vez por sala: con una consulta por intento, una semana
     * tardaba más de veinte segundos.
     */
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
                        // No se prueba con una más corta: dejaría el último turno del día
                        // siempre para la película de menor duración.
                        break;
                    }
                    if (agendas.get(sala.getId()).chocaEn(momento, fin)) {
                        momento = momento.plusMinutes(30);
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

    /**
     * La que más lejos está de los pases que le corresponden: la de menor
     * {@code asignados / peso}. Con pesos 8 y 4, la primera recibe dos pases por cada uno
     * de la segunda sin que nadie escriba esa tabla.
     */
    private Pelicula conMasDeuda(List<Pelicula> elenco, Map<Integer, Integer> asignados,
                                 double promedio) {
        return elenco.stream()
                .min(Comparator.comparingDouble(
                                (Pelicula p) -> asignados.get(p.getId()) / peso(p, promedio))
                        .thenComparing(Pelicula::getTitulo))
                .orElseThrow();
    }

    /**
     * Nunca cero: una película con puntaje 0 —recién cargada a mano, sin valorar— quedaría
     * con deuda infinita y se llevaría la grilla entera.
     */
    private double peso(Pelicula pelicula, double promedio) {
        return Math.max(puntajeConfiable(pelicula, promedio), 0.1);
    }

    // ---------- los números para poder defenderla ----------

    /**
     * El tiempo de sala que la propuesta podía usar: la ventana menos lo ya programado.
     * Sin descontarlo, una semana con las salas casi llenas daba «ocupación 27%» y se leía
     * como cine vacío. Solo cuentan las funciones que empiezan dentro de la ventana: una
     * de la mañana no le saca lugar a una grilla que arranca a las 14.
     */
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
