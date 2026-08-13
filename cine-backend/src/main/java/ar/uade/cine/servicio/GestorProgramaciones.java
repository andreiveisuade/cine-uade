package ar.uade.cine.servicio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.dominio.programaciones.ProgramacionImpl;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.ProgramacionDAO;
import ar.uade.cine.servicio.PlanProgramacion.FuncionPlanificada;

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

    /** Para nombrar contra qué choca cada fecha en un mensaje que se pueda leer. */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

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
                                          Version version, Proyeccion proyeccion, double precio) {
        Programacion grilla = armar(peliculaId, salaId, desde, hasta, horaInicio, diasSemana,
                version, proyeccion, precio);
        return planificar(grilla, peliculaDe(grilla), false);
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
                                  Version version, Proyeccion proyeccion, double precio) {
        Programacion grilla = armar(peliculaId, salaId, desde, hasta, horaInicio, diasSemana,
                version, proyeccion, precio);
        // Antes de guardar: una grilla con una película que no existe no tiene por qué
        // quedar en la base para que alguien la descubra después.
        Pelicula pelicula = peliculaDe(grilla);
        programacionDAO.guardar(grilla);
        return planificar(grilla, pelicula, true);
    }

    /**
     * La cuenta, una sola vez. {@code persistir} es lo único que separa la
     * previsualización del alta.
     *
     * <p>Cuando persiste, cada función guardada la ve la fecha siguiente: la consulta de
     * superposición sale del DAO en cada vuelta y no de una foto tomada al principio.
     */
    private PlanProgramacion planificar(Programacion grilla, Pelicula pelicula, boolean persistir) {
        List<FuncionPlanificada> plan = new ArrayList<>();
        for (LocalDateTime inicio : grilla.horarios()) {
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
                               Version version, Proyeccion proyeccion, double precio) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango tiene que empezar antes de terminar");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("Falta la hora de la función");
        }
        // Sin esto, una grilla de miércoles y jueves sobre un rango de lunes a martes se
        // daría de alta sin generar nada y sin que nadie sepa por qué.
        Programacion grilla = new ProgramacionImpl(peliculaId, salaId, desde, hasta, horaInicio,
                diasSemana, version, proyeccion, precio);
        if (grilla.horarios().isEmpty()) {
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

    public Optional<Programacion> buscar(int id) {
        return programacionDAO.buscarPorId(id);
    }

    /** Qué funciones generó esa grilla, para verlas desde el ABM. */
    public List<Funcion> funcionesDe(int id) {
        return funcionDAO.listarPorProgramacion(id);
    }
}
