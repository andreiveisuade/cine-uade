package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.programaciones.ProgramacionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.programaciones.PlanProgramacion.FuncionPlanificada;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.RecursoNoEncontrado;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorProgramaciones {

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private static final int HORIZONTE_DIAS = 14;

    private final ProgramacionRepository programacionRepository;
    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PlanProgramacion previsualizar(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        return planificar(grilla, peliculaDe(grilla), false, topeDe(grilla, reloj.hoy()));
    }

    // Recalcula R3: desde la previsualización otro pudo programar en la sala.
    public PlanProgramacion crear(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        Pelicula pelicula = peliculaDe(grilla);
        programacionRepository.save(grilla);
        return planificar(grilla, pelicula, true, topeDe(grilla, reloj.hoy()));
    }

    // Fuera de transacción para que el catch valga: en una compartida, la primera grilla
    // rota la marcaría rollback-only y voltearía la cartelera.
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
                    log.info("grilla {} extendida · {} funciones nuevas · generada hasta {}",
                            grilla.getId(), nuevas, grilla.getGeneradaHasta());
                }
            } catch (RuntimeException e) {
                log.warn("grilla {} no se pudo extender: {}", grilla.getId(), e.getMessage());
            }
        }
        return generadas;
    }

    private boolean estaAlDia(Programacion grilla, LocalDate hoy) {
        LocalDate hecho = grilla.getGeneradaHasta();
        return hecho != null && !hecho.isBefore(topeDe(grilla, hoy));
    }

    private LocalDate topeDe(Programacion grilla, LocalDate hoy) {
        return grilla.getHasta() != null ? grilla.getHasta() : hoy.plusDays(HORIZONTE_DIAS);
    }

    private PlanProgramacion planificar(Programacion grilla, Pelicula pelicula, boolean persistir,
                                        LocalDate tope) {
        LocalDate yaProcesado = grilla.getGeneradaHasta();
        List<LocalDateTime> pendientes = grilla.horarios(tope).stream()
                // R20: lo que ya pasó no se programa ni se lista, así la previsualización muestra
                // exactamente lo que el alta va a crear. No es un choque: no va a salteadas.
                .filter(inicio -> !funciones.yaPaso(inicio))
                // Por fecha procesada y no por función existente: una que chocó se reintentaría siempre.
                .filter(inicio -> yaProcesado == null || inicio.toLocalDate().isAfter(yaProcesado))
                .toList();
        // Una sola lectura de la sala para todo el rango, y no una por horario. Las funciones
        // que esta misma grilla va creando no están en la agenda: caen una por día, así que
        // solo chocarían con una película de más de un día, y a esa la frena programar(),
        // que vuelve a mirar R3 antes de guardar.
        AgendaDeSala agenda = pendientes.isEmpty() ? null : funciones.agendaDe(grilla.getSalaId(),
                pendientes.get(0),
                pendientes.get(pendientes.size() - 1).plusMinutes(pelicula.getDuracionMinutos()));
        List<FuncionPlanificada> plan = new ArrayList<>();
        for (LocalDateTime inicio : pendientes) {
            LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos());
            Optional<Funcion> choque = agenda.chocaCon(inicio, fin);
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

    private Pelicula peliculaDe(Programacion grilla) {
        return funciones.validarProgramable(grilla.getPeliculaId(), grilla.getSalaId(),
                grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio());
    }

    private Programacion armar(DatosGrilla datos) {
        // Referencias sin ir a la base: que existan lo valida validarProgramable, con su 404.
        Programacion grilla = new Programacion(peliculaRepository.getReferenceById(datos.peliculaId()),
                salaRepository.getReferenceById(datos.salaId()), datos.desde(), datos.hasta(),
                datos.horaInicio(), datos.diasSemana(), datos.version(), datos.proyeccion(), datos.precio());
        LocalDate hasta = grilla.getHasta();
        // R20: un rango cerrado que ya pasó entero se daría de alta vacío, sin nada que extender.
        if (hasta != null && grilla.horarios(hasta).stream().allMatch(funciones::yaPaso)) {
            throw new IllegalArgumentException(
                    "Todos los horarios del rango ya pasaron: la grilla no generaría funciones");
        }
        return grilla;
    }

    public void desactivar(int id) {
        Programacion grilla = buscarOFallar(id);
        grilla.desactivar();
        programacionRepository.save(grilla);
    }

    public void activar(int id) {
        Programacion grilla = buscarOFallar(id);
        grilla.activar();
        programacionRepository.save(grilla);
    }

    private Programacion buscarOFallar(int id) {
        return programacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la programación " + id));
    }

    @Transactional(readOnly = true)
    public List<Programacion> buscar(Integer peliculaId, Integer salaId, Boolean activa) {
        return programacionRepository.buscar(peliculaId, salaId, activa);
    }

    @Transactional(readOnly = true)
    public Optional<Programacion> buscar(int id) {
        return programacionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Funcion> funcionesDe(int id) {
        return funcionRepository.findByProgramacionId(id);
    }
}
