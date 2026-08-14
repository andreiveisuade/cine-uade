package ar.uade.cine.servicio.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.dominio.programaciones.ProgramacionImpl;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.servicio.programaciones.PlanProgramacion.FuncionPlanificada;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.promociones.GestorPromociones;
import ar.uade.cine.servicio.ventas.GestorPagos;
import ar.uade.cine.servicio.ventas.GestorReservas;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * La grilla del cine: "Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15 de
 * septiembre". Da de alta una programación y materializa sus funciones de una vez, para
 * que el administrador las vea enseguida y pueda corregir las que quiera.
 *
 * <p><strong>Previsualizar y después aplicar.</strong> Una grilla de quince días casi
 * siempre pisa algo: R3 dice que dos funciones de la misma sala no se superponen, y el
 * administrador no tiene por qué saber de memoria qué hay cargado en esa sala. Rechazar
 * la grilla entera por una fecha sería inusable, y guardarla sin avisar sería peor. Por
 * eso hay dos pasos: {@link #previsualizar} calcula el informe sin escribir nada, y
 * {@link #crear} hace la misma cuenta, guarda las que entran y devuelve cuáles salteó.
 *
 * <p>La cuenta se escribe <strong>una sola vez</strong>, en {@link #planificar}: lo único
 * que cambia entre los dos caminos es si persiste. Dos copias de la misma lógica se irían
 * separando, y la previsualización dejaría de predecir lo que hace el alta — que es
 * exactamente lo único que tiene que hacer.
 *
 * <p>Se apoya en {@link GestorFunciones} y no en el DAO de funciones para las reglas: R3
 * y R8 ya viven ahí, y duplicarlas acá sería tener dos definiciones de cuándo dos
 * funciones se pisan. Es la misma dependencia entre gestores que ya tiene
 * {@code GestorPagos} con {@code GestorPromociones}.
 */
public class GestorProgramaciones {

    private static final Logger LOG = LoggerFactory.getLogger(GestorProgramaciones.class);

    /** Para nombrar contra qué choca cada fecha en un mensaje que se pueda leer. */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /**
     * Cuántos días adelante se materializan las funciones de una grilla.
     *
     * <p>Una grilla abierta no tiene final, así que "generar todo el rango" deja de ser
     * posible: hay que elegir hasta dónde. Dos semanas es lo que hace falta para que la
     * cartelera se vea llena y el cliente pueda comprar con anticipación, y lo bastante
     * poco como para que cambiar de opinión —dar de baja la grilla, subir el precio— no
     * obligue a tocar cientos de funciones ya vendidas.
     */
    private static final int HORIZONTE_DIAS = 14;

    private final ProgramacionDAO programacionDAO;
    private final FuncionDAO funcionDAO;
    private final GestorFunciones funciones;

    public GestorProgramaciones(ProgramacionDAO programacionDAO, FuncionDAO funcionDAO,
                                GestorFunciones funciones) {
        this.programacionDAO = programacionDAO;
        this.funcionDAO = funcionDAO;
        this.funciones = funciones;
    }

    /**
     * Qué haría el alta, sin tocar la base. La grilla que devuelve el informe existe solo
     * en memoria: no se guarda y no tiene id.
     */
    public PlanProgramacion previsualizar(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                                          LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                                          Version version, Proyeccion proyeccion, Dinero precio) {
        Programacion grilla = armar(peliculaId, salaId, desde, hasta, horaInicio, diasSemana,
                version, proyeccion, precio);
        return planificar(grilla, peliculaDe(grilla), false, topeDe(grilla, LocalDate.now()));
    }

    /**
     * Da de alta la grilla y genera sus funciones, salteando las fechas que chocan.
     *
     * <p><strong>Vuelve a validar R3 desde el DAO: no confía en la previsualización.</strong>
     * Entre que el administrador miró el informe y apretó confirmar, otro pudo haber
     * programado algo en esa sala. Es el mismo argumento por el que el sistema valida en
     * el gestor <em>y</em> restringe en la base: la validación temprana da el mensaje
     * claro del caso normal, la tardía cubre la carrera. Por eso {@code crear} no recibe
     * el plan previsualizado sino los mismos datos, y lo recalcula.
     *
     * <p>La grilla se guarda aunque todas sus fechas choquen. Es una decisión del cine
     * —"Matrix va en la Sala 1 a las 20:30"— y sigue siendo cierta aunque hoy no entre
     * ninguna función; el informe dice qué pasó.
     */
    public PlanProgramacion crear(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                                  LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                                  Version version, Proyeccion proyeccion, Dinero precio) {
        Programacion grilla = armar(peliculaId, salaId, desde, hasta, horaInicio, diasSemana,
                version, proyeccion, precio);
        // Antes de guardar: una grilla con una película que no existe no tiene por qué
        // quedar en la base para que alguien la descubra después.
        Pelicula pelicula = peliculaDe(grilla);
        programacionDAO.guardar(grilla);
        return planificar(grilla, pelicula, true, topeDe(grilla, LocalDate.now()));
    }

    /**
     * Materializa lo que las grillas activas todavía no generaron, hasta el horizonte.
     *
     * <p>No hay proceso de fondo ni scheduler: la extensión la hace quien consulta, igual
     * que la expiración de reservas en {@code GestorReservas}. Un cine cuya cartelera nadie
     * mira no necesita funciones nuevas; el día que alguien la mira, aparecen.
     *
     * <p>Es <strong>idempotente</strong>: cada grilla recuerda hasta qué fecha se procesó,
     * así que llamarlo mil veces en el mismo día hace trabajo una sola vez. Eso es lo que
     * permite colgarlo de una lectura sin pensar en cuántas veces se lee.
     *
     * <p>Acá es donde {@code activa} deja de ser decorativo: una grilla dada de baja se
     * saltea, y con eso deja de generar funciones nuevas sin tocar las que ya vendió.
     *
     * @return cuántas funciones se generaron
     */
    public int extenderActivas(LocalDate hoy) {
        int generadas = 0;
        for (Programacion grilla : programacionDAO.listar()) {
            if (!grilla.estaActiva() || estaAlDia(grilla, hoy)) {
                continue;
            }
            try {
                int nuevas = planificar(grilla, peliculaDe(grilla), true, topeDe(grilla, hoy))
                        .programables().size();
                generadas += nuevas;
                if (nuevas > 0) {
                    // Esta línea es la que vuelve visible al mecanismo. La extensión pasa
                    // sola, colgada de una lectura, y sin registro un cine que amanece con
                    // funciones nuevas no tiene forma de decir de dónde salieron.
                    LOG.info("grilla {} extendida · {} funciones nuevas · generada hasta {}",
                            grilla.getId(), nuevas, grilla.getGeneradaHasta());
                }
            } catch (RuntimeException e) {
                // Una grilla que dejó de ser válida —la película se dio de baja, la sala
                // cambió de tipo— no puede romper la pantalla de quien pasaba por acá a
                // mirar la cartelera. Se saltea y sigue; el ABM de grillas es donde eso se
                // ve y se corrige. Pero se registra: un error que se traga en silencio es
                // una grilla que deja de generar y nadie se entera hasta que falta la
                // función.
                LOG.warn("grilla {} no se pudo extender: {}", grilla.getId(), e.getMessage());
            }
        }
        return generadas;
    }

    /** Ya se procesaron todas las fechas que le tocan. */
    private boolean estaAlDia(Programacion grilla, LocalDate hoy) {
        LocalDate hecho = grilla.getGeneradaHasta();
        return hecho != null && !hecho.isBefore(topeDe(grilla, hoy));
    }

    /**
     * Hasta qué fecha materializar esta grilla.
     *
     * <p>El horizonte se aplica <strong>solo a las grillas abiertas</strong>, que son las
     * únicas que lo necesitan: sin final, "generar todo el rango" no significa nada. Una
     * grilla cerrada genera el suyo entero de una vez, como siempre. Recortarla sería
     * peor que inútil: el administrador puso las fechas a mano —una temporada, un ciclo—
     * y el informe de choques le sirve justo en ese momento, no dentro de dos semanas.
     */
    private LocalDate topeDe(Programacion grilla, LocalDate hoy) {
        return grilla.getHasta() != null ? grilla.getHasta() : hoy.plusDays(HORIZONTE_DIAS);
    }

    /**
     * La cuenta, una sola vez. {@code persistir} es lo único que separa la
     * previsualización del alta.
     *
     * <p>Cuando persiste, cada función guardada la ve la fecha siguiente: la consulta de
     * superposición sale del DAO en cada vuelta y no de una foto tomada al principio.
     */
    private PlanProgramacion planificar(Programacion grilla, Pelicula pelicula, boolean persistir,
                                        LocalDate tope) {
        LocalDate yaProcesado = grilla.getGeneradaHasta();
        List<FuncionPlanificada> plan = new ArrayList<>();
        for (LocalDateTime inicio : grilla.horarios(tope)) {
            // Las fechas de vueltas anteriores no se vuelven a mirar. Se filtra por fecha
            // procesada y no por "¿existe ya la función?" porque una que chocó no generó
            // nada: preguntarle al DAO la daría por pendiente y se reintentaría para
            // siempre, y el informe la volvería a listar cada vez.
            if (yaProcesado != null && !inicio.toLocalDate().isAfter(yaProcesado)) {
                continue;
            }
            LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
            Optional<Funcion> choque = funciones.superpuestaEn(grilla.getSalaId(), inicio, fin);
            if (choque.isPresent()) {
                plan.add(new FuncionPlanificada(inicio, true,
                        "la sala ya tiene la función " + choque.get().getId() + " a las "
                        + choque.get().getInicio().format(MOMENTO)));
                continue;
            }
            if (persistir) {
                funciones.programar(grilla.getPeliculaId(), grilla.getSalaId(), inicio,
                        grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio(),
                        grilla.getId());
            }
            plan.add(new FuncionPlanificada(inicio, false, null));
        }
        if (persistir) {
            // Se marca el tope y no la última función generada: las fechas que chocaron
            // también quedan procesadas. Si se guardara la última que entró, la próxima
            // vuelta volvería a evaluar los choques y a listarlos como si fueran nuevos.
            grilla.setGeneradaHasta(tope);
            programacionDAO.actualizar(grilla);
        }
        return new PlanProgramacion(grilla, plan);
    }

    /**
     * Valida de una vez lo que no depende de la fecha —que existan película y sala, R8 y
     * el precio— y devuelve la película, que es de donde sale la duración para calcular
     * cuándo termina cada función. Si la sala no proyecta en 3D, no hay ninguna fecha del
     * rango en la que sí: preguntarlo una vez por grilla y no una vez por función es lo
     * que hace que previsualizar falle con el mismo mensaje que fallaría el alta.
     */
    private Pelicula peliculaDe(Programacion grilla) {
        return funciones.validarProgramable(grilla.getPeliculaId(), grilla.getSalaId(),
                grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio());
    }

    /** Lo que valida la grilla en sí; lo que valida cada función lo pone GestorFunciones. */
    private Programacion armar(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                               LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                               Version version, Proyeccion proyeccion, Dinero precio) {
        if (desde == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio");
        }
        // hasta null es una grilla abierta y es válido; lo que no se admite es un rango
        // dado vuelta.
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango tiene que empezar antes de terminar");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("Falta la hora de la función");
        }
        // Sin esto, una grilla de miércoles y jueves sobre un rango de lunes a martes se
        // daría de alta sin generar nada y sin que nadie sepa por qué. Se mira contra el
        // propio hasta y no contra el horizonte: el error es del rango que eligió el
        // administrador, y una grilla abierta siempre termina cayendo en algún día.
        Programacion grilla = new ProgramacionImpl(peliculaId, salaId, desde, hasta, horaInicio,
                diasSemana, version, proyeccion, precio);
        if (hasta != null && grilla.horarios(hasta).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ningún día del rango cae en los días elegidos: la grilla no generaría funciones");
        }
        return grilla;
    }

    /**
     * Da de baja la grilla. Las funciones ya generadas <strong>quedan</strong>: pueden
     * tener reservas vendidas, y en este sistema nada que haya producido ventas se borra.
     * Lo que la baja evita es que se generen nuevas.
     */
    public void desactivar(int id) {
        cambiarEstado(id, false);
    }

    public void activar(int id) {
        cambiarEstado(id, true);
    }

    private void cambiarEstado(int id, boolean activa) {
        Programacion grilla = programacionDAO.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la programación " + id));
        grilla.setActiva(activa);
        programacionDAO.actualizar(grilla);
    }

    public List<Programacion> listar() {
        return programacionDAO.listar();
    }

    /**
     * Las grillas que cumplen los criterios. Cualquier parámetro en {@code null} no filtra.
     *
     * <p>{@code activa} en {@code true} es la pregunta que más se hace acá: cuáles están
     * generando funciones hoy. Las dadas de baja no se borran nunca —siguen explicando las
     * funciones que crearon— así que la lista solo crece y el filtro es lo que la mantiene
     * usable.
     */
    public List<Programacion> buscar(Integer peliculaId, Integer salaId, Boolean activa) {
        return programacionDAO.listar().stream()
                .filter(p -> peliculaId == null || p.getPeliculaId() == peliculaId)
                .filter(p -> salaId == null || p.getSalaId() == salaId)
                .filter(p -> activa == null || p.estaActiva() == activa)
                .toList();
    }

    public Optional<Programacion> buscar(int id) {
        return programacionDAO.buscarPorId(id);
    }

    /** Qué funciones generó esa grilla, para verlas desde el ABM. */
    public List<Funcion> funcionesDe(int id) {
        return funcionDAO.listarPorProgramacion(id);
    }
}
