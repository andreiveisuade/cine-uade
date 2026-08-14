package ar.uade.cine.servicio;

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

import ar.uade.cine.dominio.cartelera.EstadoRevision;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.servicio.PropuestaGrilla.IndicadoresGrilla;
import ar.uade.cine.servicio.PropuestaGrilla.PaseSugerido;

/**
 * Arma la grilla de la semana: elige qué películas se dan y las reparte en las salas.
 *
 * <p>Es la respuesta a un problema que apareció con el importador de TMDB. Antes el
 * catálogo tenía cuatro títulos y programarlos era obvio; ahora entran dieciocho por
 * corrida y hay que <strong>elegir</strong>. Elegir a ojo termina siempre igual: se
 * programa lo más conocido, la cartelera queda con cuatro películas de acción y el
 * miércoles a la tarde no hay nada que ver para quien no va al cine a eso.
 *
 * <p>El planificador optimiza tres cosas a la vez, y ninguna sola alcanza:
 * <ul>
 *   <li><strong>Puntaje</strong>: dar lo mejor valorado. Solo con esto, la grilla es
 *       monotemática — las mejor puntuadas de una temporada suelen parecerse.</li>
 *   <li><strong>Diversidad</strong>: cubrir géneros distintos. Solo con esto, entra
 *       cualquier cosa con tal de que sea de un género que falta.</li>
 *   <li><strong>Ocupación</strong>: no dejar la sala vacía. Solo con esto, gana la
 *       película más corta, que es la que más veces entra en el día.</li>
 * </ul>
 *
 * <p><strong>No reescribe R3.</strong> Antes de proponer un pase le pregunta a
 * {@link GestorFunciones#superpuestaEn} si esa sala está libre —con la limpieza incluida—,
 * igual que hace {@link GestorProgramaciones}. Por eso una grilla propuesta sobre salas
 * que ya tienen funciones cargadas las respeta en vez de pisarlas.
 *
 * <p>Es <strong>determinista</strong>: los mismos criterios sobre los mismos datos dan la
 * misma propuesta. Eso es lo que permite que previsualizar y aplicar sean dos llamadas
 * distintas sin que el resultado cambie en el medio, que es el mismo par que ya tienen las
 * programaciones con su {@code /previsualizar}.
 */
public class PlanificadorGrilla {

    /**
     * Cuánto vale, en puntos, que una película traiga géneros que todavía no están en el
     * elenco.
     *
     * <p>Dos puntos sobre diez es deliberadamente caro: alcanza para que una comedia de
     * 7,0 le gane a la cuarta película de acción de 8,5, que es exactamente la decisión
     * que el planificador existe para tomar. Más bajo y la diversidad no se nota; más alto
     * y entra cualquier cosa con tal de tapar un género vacío.
     *
     * <p>El bono no es lineal en la cantidad de géneros nuevos sino que <strong>crece cada
     * vez menos</strong>: el primero vale los dos puntos enteros, el segundo suma menos, el
     * cuarto casi nada. Lo pide el dato con el que trabajamos: los géneros salen de TMDB y
     * son generosos —una película figura a la vez como acción, animación, ciencia ficción y
     * comedia—. Con un bono lineal esa película se llevaría ocho puntos, tanto como el
     * puntaje máximo posible, y le ganaría a la mejor del catálogo por tener más etiquetas
     * puestas y no por aportar cuatro veces más variedad.
     */
    private static final double BONO_GENERO_NUEVO = 2.0;

    private final PeliculaDAO peliculaDAO;
    private final SalaDAO salaDAO;
    private final GestorFunciones funciones;

    public PlanificadorGrilla(PeliculaDAO peliculaDAO, SalaDAO salaDAO, GestorFunciones funciones) {
        this.peliculaDAO = peliculaDAO;
        this.salaDAO = salaDAO;
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
        List<PaseSugerido> pases = repartir(elenco, criterios);
        return new PropuestaGrilla(elenco, pases, medir(elenco, pases, criterios));
    }

    /**
     * La misma propuesta, pero creando las funciones de verdad.
     *
     * <p>Vuelve a calcularla en vez de recibirla hecha, por lo mismo que
     * {@code GestorProgramaciones} recalcula al aplicar: si el cliente pudiera mandar la
     * propuesta de vuelta, podría mandar una distinta de la que se le mostró —o una vieja,
     * armada antes de que alguien cargara otra función en esa sala— y el alta escribiría
     * algo que ninguna regla revisó. Como el algoritmo es determinista, recalcular sobre
     * los mismos criterios da lo mismo que se previsualizó.
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
     * Elige el elenco de la semana con un goloso que mira puntaje y géneros a la vez.
     *
     * <p>La primera elección es la mejor película a secas. De ahí en adelante, cada vuelta
     * toma la de mayor <em>valor</em>: su puntaje más un bono por los géneros que todavía
     * no están cubiertos. Al elegirla, esos géneros pasan a estar cubiertos y las que
     * venían apoyadas en ellos valen menos en la vuelta siguiente, así que cada una tiene
     * que ganarse el lugar contra lo que ya hay.
     *
     * <p>Un goloso y no la combinación óptima: elegir 8 de 18 son 43.758 combinaciones y
     * evaluarlas todas es posible, pero la diferencia con esto es de decimales y el
     * resultado deja de poder explicarse en una pantalla. Que el encargado entienda por
     * qué entró cada película vale más acá que el óptimo exacto.
     */
    private List<Pelicula> elegirElenco(int cuantas) {
        List<Pelicula> candidatas = new ArrayList<>(peliculaDAO.listar().stream()
                .filter(p -> p.getEstadoRevision() == EstadoRevision.CONFIRMADA)
                .filter(p -> p.getDuracionMinutos() > 0)
                .toList());

        List<Pelicula> elenco = new ArrayList<>();
        Set<Genero> cubiertos = new HashSet<>();
        while (elenco.size() < cuantas && !candidatas.isEmpty()) {
            Set<Genero> yaCubiertos = Set.copyOf(cubiertos);
            Pelicula mejor = candidatas.stream()
                    .max(Comparator.comparingDouble((Pelicula p) -> valor(p, yaCubiertos, elenco.isEmpty()))
                            // Desempate por título: sin esto, dos películas con el mismo
                            // valor podrían salir en cualquier orden y la propuesta dejaría
                            // de ser reproducible.
                            .thenComparing(Pelicula::getTitulo, Comparator.reverseOrder()))
                    .orElseThrow();
            elenco.add(mejor);
            cubiertos.addAll(mejor.getGeneros());
            candidatas.remove(mejor);
        }
        return elenco;
    }

    /**
     * Cuánto vale esta película <em>ahora</em>, con este elenco a medio armar.
     *
     * <p>Con el elenco vacío no hay bono, y no es un caso borde: el bono mide cuánta
     * variedad <em>agrega</em> una película a lo que ya se eligió, y cuando no se eligió
     * nada todavía no hay nada a lo que agregarle. Aplicarlo igual premiaría a la película
     * con más etiquetas de TMDB, que es una propiedad de cómo está catalogada y no del
     * aporte que hace. Así la primera elección es la mejor película a secas, y de ahí en
     * adelante cada una tiene que ganarse el lugar contra lo que ya hay.
     *
     * <p>La raíz es lo que hace que el bono crezca cada vez menos. Sin ella, dos géneros
     * nuevos valen el doble que uno y cuatro el cuádruple, que es exactamente la cuenta que
     * no queremos.
     */
    private double valor(Pelicula pelicula, Set<Genero> cubiertos, boolean primera) {
        if (primera) {
            return pelicula.getPuntaje();
        }
        long nuevos = pelicula.getGeneros().stream().filter(g -> !cubiertos.contains(g)).count();
        return pelicula.getPuntaje() + BONO_GENERO_NUEVO * Math.sqrt(nuevos);
    }

    // ---------- etapa 2: dónde y cuándo ----------

    /**
     * Llena cada sala, día por día, desde la apertura hasta que no entre una función más.
     *
     * <p>Qué película va en cada hueco lo decide un reparto <strong>proporcional al
     * puntaje</strong>: en cada paso entra la que tiene mayor «deuda», o sea la que menos
     * pases lleva en relación a lo que merece. Así la mejor película de la semana termina
     * con cuatro o cinco funciones diarias y la octava con una, sin que haya que escribir
     * esa tabla a mano ni repartir en partes iguales —que sería tratar igual a lo que el
     * público no valora igual.
     *
     * <p>Los huecos que ya están ocupados no se saltean en silencio: si la sala tiene una
     * función cargada, {@code superpuestaEn} la detecta y el planificador avanza al
     * horario siguiente. Es la misma regla R3 de siempre, preguntada y no reescrita.
     */
    private List<PaseSugerido> repartir(List<Pelicula> elenco, CriteriosGrilla criterios) {
        List<Sala> salas = salaDAO.listar();
        List<PaseSugerido> pases = new ArrayList<>();
        Map<Integer, Integer> asignados = new HashMap<>();
        elenco.forEach(p -> asignados.put(p.getId(), 0));

        for (int dia = 0; dia < criterios.dias(); dia++) {
            LocalDate fecha = criterios.desde().plusDays(dia);
            for (Sala sala : salas) {
                LocalDateTime momento = fecha.atTime(criterios.apertura());
                LocalDateTime limite = fecha.atTime(criterios.cierreEfectivo());

                while (momento.isBefore(limite)) {
                    Pelicula elegida = conMasDeuda(elenco, asignados);
                    LocalDateTime fin = momento.plusMinutes(elegida.getDuracionMinutos());
                    if (fin.isAfter(limite)) {
                        // No entra completa antes de cerrar: la sala se da por llena. No se
                        // prueba con una más corta a propósito, porque eso dejaría el
                        // último turno del día siempre para la película de menor duración.
                        break;
                    }
                    if (funciones.superpuestaEn(sala.getId(), momento, fin).isPresent()) {
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
     * La que más lejos está de los pases que le corresponden por puntaje.
     *
     * <p>La deuda es {@code asignados / peso}: cuanto más chico ese número, más le deben.
     * Con pesos 8 y 4, la primera recibe dos pases por cada uno de la segunda sin que
     * nadie escriba «dos por uno» en ningún lado.
     */
    private Pelicula conMasDeuda(List<Pelicula> elenco, Map<Integer, Integer> asignados) {
        return elenco.stream()
                .min(Comparator.comparingDouble((Pelicula p) -> asignados.get(p.getId()) / peso(p))
                        .thenComparing(Pelicula::getTitulo))
                .orElseThrow();
    }

    /**
     * Nunca cero: una película con puntaje 0 —recién cargada a mano, sin valorar— quedaría
     * con deuda infinita y se llevaría la grilla entera.
     */
    private double peso(Pelicula pelicula) {
        return Math.max(pelicula.getPuntaje(), 0.1);
    }

    // ---------- los números para poder defenderla ----------

    /**
     * Cuánto tiempo de sala hay realmente para llenar: la ventana entera menos lo que ya
     * está programado.
     *
     * <p>Descontarlo es lo que hace que la ocupación signifique lo que dice. Sin esto, una
     * semana con las salas casi llenas daba «ocupación 27%» —la propuesta apenas encontraba
     * huecos— y se leía como que el cine estaba vacío, cuando era exactamente al revés. El
     * denominador tiene que ser el tiempo que la propuesta <em>podía</em> usar, no el que
     * habría tenido con las salas vacías.
     *
     * <p>Cuenta las funciones que empiezan dentro de la ventana horaria: una función de la
     * mañana ocupa la sala, pero no le saca lugar a una grilla que arranca a las 14.
     */
    private int minutosLibres(CriteriosGrilla criterios) {
        long minutosPorDia = Duration.between(criterios.apertura(), criterios.cierreEfectivo()).toMinutes();
        int ventana = (int) (minutosPorDia * criterios.dias() * salaDAO.listar().size());

        LocalDate hasta = criterios.desde().plusDays(criterios.dias() - 1L);
        int ocupados = funciones.buscar(null, null, criterios.desde(), hasta).stream()
                .filter(f -> !f.getInicio().toLocalTime().isBefore(criterios.apertura()))
                .filter(f -> f.getInicio().toLocalTime().isBefore(criterios.cierreEfectivo()))
                .mapToInt(f -> peliculaDAO.buscarPorId(f.getPeliculaId())
                        .map(Pelicula::getDuracionMinutos)
                        .orElse(0))
                .sum();

        // Nunca negativo: con salas sobrevendidas por funciones cargadas a mano, el
        // descuento puede pasarse de la ventana y una ocupación negativa no significa nada.
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
        if (criterios.precio() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (!criterios.apertura().isBefore(criterios.cierreEfectivo())) {
            throw new IllegalArgumentException("El cine tiene que cerrar después de abrir");
        }
        if (salaDAO.listar().isEmpty()) {
            throw new IllegalArgumentException("No hay salas cargadas para programar");
        }
    }
}
