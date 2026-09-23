package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.repository.FuncionRepository;
import ar.uade.cine.repository.ProgramacionRepository;
import ar.uade.cine.service.programaciones.PlanProgramacion.FuncionPlanificada;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.infrastructure.reloj.Reloj;

/**
 * Grillas recurrentes ("Matrix en la Sala 1, todos los días a las 20:30") y sus funciones.
 * Una grilla casi siempre pisa algo (R3), así que {@link #previsualizar} informa sin
 * escribir y {@link #crear} guarda las que entran y dice cuáles salteó; ambas usan
 * {@link #planificar}. R3 y R8 los decide {@link GestorFunciones}.
 */
@Service
@Transactional
public class GestorProgramaciones {

    private static final Logger LOG = LoggerFactory.getLogger(GestorProgramaciones.class);

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /**
     * Días que se materializa una grilla abierta: alcanza para comprar con anticipación
     * sin atar cientos de funciones vendidas a una decisión que puede cambiar.
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

    /** La grilla del informe queda solo en memoria, sin id. */
    public PlanProgramacion previsualizar(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        return planificar(grilla, peliculaDe(grilla), false, topeDe(grilla, reloj.hoy()));
    }

    /**
     * Recalcula R3 porque desde la previsualización otro pudo programar en la sala. La
     * grilla se guarda aunque choquen todas sus fechas: sigue siendo una decisión del cine.
     */
    public PlanProgramacion crear(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        Pelicula pelicula = peliculaDe(grilla);
        programacionRepository.save(grilla);
        return planificar(grilla, pelicula, true, topeDe(grilla, reloj.hoy()));
    }

    /**
     * Materializa hasta el horizonte lo que falta. Sin scheduler: lo dispara quien consulta
     * la cartelera, e idempotente porque cada grilla recuerda hasta dónde se procesó.
     *
     * <p>Corre <strong>fuera</strong> de transacción para que el {@code catch} valga: en una
     * compartida, la primera grilla rota la marcaría rollback-only y voltearía la cartelera.
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
                    // Pasa colgada de una lectura: sin log no se sabe de dónde salieron.
                    LOG.info("grilla {} extendida · {} funciones nuevas · generada hasta {}",
                            grilla.getId(), nuevas, grilla.getGeneradaHasta());
                }
            } catch (RuntimeException e) {
                // Una grilla inválida no rompe la cartelera; se corrige desde el ABM.
                LOG.warn("grilla {} no se pudo extender: {}", grilla.getId(), e.getMessage());
            }
        }
        return generadas;
    }

    private boolean estaAlDia(Programacion grilla, LocalDate hoy) {
        LocalDate hecho = grilla.getGeneradaHasta();
        return hecho != null && !hecho.isBefore(topeDe(grilla, hoy));
    }

    /** Una grilla cerrada genera su rango entero: el informe de choques sirve ahora. */
    private LocalDate topeDe(Programacion grilla, LocalDate hoy) {
        return grilla.getHasta() != null ? grilla.getHasta() : hoy.plusDays(HORIZONTE_DIAS);
    }

    /** {@code persistir} es lo único que separa previsualizar de crear. */
    private PlanProgramacion planificar(Programacion grilla, Pelicula pelicula, boolean persistir,
                                        LocalDate tope) {
        LocalDate yaProcesado = grilla.getGeneradaHasta();
        List<FuncionPlanificada> plan = new ArrayList<>();
        for (LocalDateTime inicio : grilla.horarios(tope)) {
            // Por fecha procesada y no por función existente: una que chocó se reintentaría siempre.
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
            // El tope y no la última generada: las que chocaron también quedan procesadas.
            grilla.setGeneradaHasta(tope);
            programacionRepository.save(grilla);
        }
        return new PlanProgramacion(grilla, plan);
    }

    /** Valida una vez lo que no depende de la fecha (película, sala, R8, precio). */
    private Pelicula peliculaDe(Programacion grilla) {
        return funciones.validarProgramable(grilla.getPeliculaId(), grilla.getSalaId(),
                grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio());
    }

    private Programacion armar(DatosGrilla datos) {
        LocalDate desde = datos.desde();
        LocalDate hasta = datos.hasta();
        if (desde == null) {
            throw new IllegalArgumentException("Falta la fecha de inicio");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango tiene que empezar antes de terminar");
        }
        if (datos.horaInicio() == null) {
            throw new IllegalArgumentException("Falta la hora de la función");
        }
        // Si no, una grilla de miércoles sobre un rango lunes-martes se guardaría vacía.
        Programacion grilla = new Programacion(datos.peliculaId(), datos.salaId(), desde, hasta,
                datos.horaInicio(), datos.diasSemana(), datos.version(), datos.proyeccion(), datos.precio());
        if (hasta != null && grilla.horarios(hasta).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ningún día del rango cae en los días elegidos: la grilla no generaría funciones");
        }
        return grilla;
    }

    /** Las funciones ya generadas quedan: pueden tener ventas. Solo deja de generar nuevas. */
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

    /** {@code null} no filtra. */
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

    public List<Funcion> funcionesDe(int id) {
        return funcionRepository.findByProgramacionId(id);
    }
}
