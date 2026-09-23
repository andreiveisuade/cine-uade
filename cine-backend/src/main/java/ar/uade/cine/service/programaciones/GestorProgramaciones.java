package ar.uade.cine.service.programaciones;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ProgramacionRepository;
import ar.uade.cine.service.programaciones.PlanProgramacion.FuncionPlanificada;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * La grilla del cine: "Matrix en la Sala 1, todos los días a las 20:30, del 1 al 15".
 * Da de alta la programación y materializa sus funciones.
 *
 * <p><strong>Previsualizar y después aplicar.</strong> Una grilla de quince días casi
 * siempre pisa algo (R3), y ni rechazarla entera ni guardarla sin avisar sirve. Por eso
 * {@link #previsualizar} calcula el informe sin escribir y {@link #crear} hace la misma
 * cuenta, guarda las que entran y dice cuáles salteó. La cuenta está una sola vez, en
 * {@link #planificar}: lo único que cambia es si persiste.
 *
 * <p>Las reglas por función (R3, R8) se le preguntan a {@link GestorFunciones}, no se
 * repiten acá.
 */
@Service
@Transactional
public class GestorProgramaciones {

    private static final Logger LOG = LoggerFactory.getLogger(GestorProgramaciones.class);

    /** Para nombrar contra qué choca cada fecha en un mensaje que se pueda leer. */
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /**
     * Hasta cuántos días adelante se materializa una grilla abierta. Dos semanas: lo que
     * hace falta para comprar con anticipación, y lo bastante poco para que cambiar de
     * opinión no obligue a tocar cientos de funciones ya vendidas.
     */
    private static final int HORIZONTE_DIAS = 14;

    private final ProgramacionRepository programacionRepository;
    private final FuncionRepository funcionRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    public GestorProgramaciones(ProgramacionRepository programacionRepository, FuncionRepository funcionRepository,
                                GestorFunciones funciones, Reloj reloj) {
        this.programacionRepository = programacionRepository;
        this.funcionRepository = funcionRepository;
        this.funciones = funciones;
        this.reloj = reloj;
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
        return planificar(grilla, peliculaDe(grilla), false, topeDe(grilla, reloj.hoy()));
    }

    /**
     * Da de alta la grilla y genera sus funciones, salteando las fechas que chocan.
     *
     * <p>Recalcula R3 en vez de confiar en la previsualización: entre que el encargado
     * miró el informe y confirmó, otro pudo programar en esa sala. La grilla se guarda
     * aunque todas sus fechas choquen: "Matrix va en la Sala 1 a las 20:30" sigue siendo
     * una decisión del cine, y el informe dice qué pasó.
     */
    public PlanProgramacion crear(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                                  LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                                  Version version, Proyeccion proyeccion, Dinero precio) {
        Programacion grilla = armar(peliculaId, salaId, desde, hasta, horaInicio, diasSemana,
                version, proyeccion, precio);
        // Antes de guardar: una grilla con una película inexistente no tiene por qué quedar en la base.
        Pelicula pelicula = peliculaDe(grilla);
        programacionRepository.save(grilla);
        return planificar(grilla, pelicula, true, topeDe(grilla, reloj.hoy()));
    }

    /**
     * Materializa lo que las grillas activas todavía no generaron, hasta el horizonte.
     * Sin scheduler: lo hace quien consulta la cartelera, y es idempotente porque cada
     * grilla recuerda hasta qué fecha se procesó.
     *
     * <p>Corre <strong>fuera</strong> de transacción para que el {@code catch} valga:
     * cada grilla escribe en la transacción de {@code GestorFunciones}, y si una falla
     * las demás siguen. Adentro de una compartida, la primera rota marcaría rollback-only
     * y una grilla inválida voltearía la pantalla de cartelera.
     *
     * @return cuántas funciones se generaron
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int extenderActivas(LocalDate hoy) {
        int generadas = 0;
        for (Programacion grilla : programacionRepository.findByActivaTrue()) {
            if (estaAlDia(grilla, hoy)) {
                continue;
            }
            try {
                int nuevas = planificar(grilla, peliculaDe(grilla), true, topeDe(grilla, hoy))
                        .programables().size();
                generadas += nuevas;
                if (nuevas > 0) {
                    // Pasa sola, colgada de una lectura: sin registro, un cine que amanece
                    // con funciones nuevas no puede decir de dónde salieron.
                    LOG.info("grilla {} extendida · {} funciones nuevas · generada hasta {}",
                            grilla.getId(), nuevas, grilla.getGeneradaHasta());
                }
            } catch (RuntimeException e) {
                // Una grilla que dejó de ser válida no puede romper la cartelera de quien
                // pasaba a mirarla; se corrige desde el ABM. Pero queda registrado.
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
     * Hasta qué fecha materializar. El horizonte es solo para las grillas abiertas; una
     * cerrada genera su rango entero, porque el informe de choques le sirve al encargado
     * ahora y no dentro de dos semanas.
     */
    private LocalDate topeDe(Programacion grilla, LocalDate hoy) {
        return grilla.getHasta() != null ? grilla.getHasta() : hoy.plusDays(HORIZONTE_DIAS);
    }

    /**
     * La cuenta, una sola vez: {@code persistir} es lo único que separa previsualizar
     * de crear. Cuando persiste, cada función guardada la ve la fecha siguiente.
     */
    private PlanProgramacion planificar(Programacion grilla, Pelicula pelicula, boolean persistir,
                                        LocalDate tope) {
        LocalDate yaProcesado = grilla.getGeneradaHasta();
        List<FuncionPlanificada> plan = new ArrayList<>();
        for (LocalDateTime inicio : grilla.horarios(tope)) {
            // Se filtra por fecha procesada y no por "¿existe la función?": una que chocó no
            // generó nada y se reintentaría para siempre.
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
            // Se marca el tope y no la última generada: las fechas que chocaron también
            // quedan procesadas, si no la próxima vuelta las listaría como nuevas.
            grilla.setGeneradaHasta(tope);
            programacionRepository.save(grilla);
        }
        return new PlanProgramacion(grilla, plan);
    }

    /**
     * Valida una vez lo que no depende de la fecha (película, sala, R8, precio) y devuelve
     * la película, de donde sale la duración. Si la sala no proyecta en 3D no hay ninguna
     * fecha en la que sí: así previsualizar falla con el mismo mensaje que el alta.
     */
    private Pelicula peliculaDe(Programacion grilla) {
        return funciones.validarProgramable(grilla.getPeliculaId(), grilla.getSalaId(),
                grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio());
    }

    /** Lo que valida la grilla en sí; lo de cada función lo pone GestorFunciones. */
    private Programacion armar(int peliculaId, int salaId, LocalDate desde, LocalDate hasta,
                               LocalTime horaInicio, Set<DayOfWeek> diasSemana,
                               Version version, Proyeccion proyeccion, Dinero precio) {
        if (desde == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio");
        }
        // hasta null es una grilla abierta; lo que no se admite es un rango dado vuelta.
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango tiene que empezar antes de terminar");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("Falta la hora de la función");
        }
        // Una grilla de miércoles sobre un rango de lunes a martes se daría de alta sin
        // generar nada. Se mira contra el propio hasta: una abierta siempre cae en algún día.
        Programacion grilla = new Programacion(peliculaId, salaId, desde, hasta, horaInicio,
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
        Programacion grilla = programacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la programación " + id));
        grilla.setActiva(activa);
        programacionRepository.save(grilla);
    }

    public List<Programacion> listar() {
        return programacionRepository.findAll();
    }

    /**
     * Las grillas que cumplen los criterios; {@code null} no filtra. Las dadas de baja no
     * se borran nunca —explican las funciones que crearon—, así que la lista solo crece.
     */
    public List<Programacion> buscar(Integer peliculaId, Integer salaId, Boolean activa) {
        return programacionRepository.findAll().stream()
                .filter(p -> peliculaId == null || p.getPeliculaId() == peliculaId)
                .filter(p -> salaId == null || p.getSalaId() == salaId)
                .filter(p -> activa == null || p.estaActiva() == activa)
                .toList();
    }

    public Optional<Programacion> buscar(int id) {
        return programacionRepository.findById(id);
    }

    /** Qué funciones generó esa grilla, para verlas desde el ABM. */
    public List<Funcion> funcionesDe(int id) {
        return funcionRepository.findByProgramacionId(id);
    }
}
