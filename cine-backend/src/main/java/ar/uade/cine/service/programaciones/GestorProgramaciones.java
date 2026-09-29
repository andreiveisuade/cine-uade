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

// Alta, baja y extensión de grillas; genera cada función por GestorFunciones para no reescribir R3 ni R20.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorProgramaciones {

    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ProgramacionRepository programacionRepository;
    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PlanProgramacion previsualizar(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        return planDe(grilla, peliculaDe(grilla), grilla.topePara(reloj.hoy()));
    }

    // Recalcula R3: desde la previsualización otro pudo programar en la sala.
    public PlanProgramacion crear(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        Pelicula pelicula = peliculaDe(grilla);
        programacionRepository.save(grilla);
        return generar(grilla, pelicula, grilla.topePara(reloj.hoy()));
    }

    // Fuera de transacción para que el catch valga: en una compartida, la primera grilla
    // rota la marcaría rollback-only y voltearía la cartelera.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int extenderActivas(LocalDate hoy) {
        int generadas = 0;
        for (Programacion grilla : programacionRepository.findByActivaTrue()) {
            if (grilla.estaAlDia(hoy)) {
                continue;
            }
            try {
                int nuevas = generar(grilla, peliculaDe(grilla), grilla.topePara(hoy)).programables().size();
                // Sin transacción la grilla está detached: el dirty checking no ve el avance.
                programacionRepository.save(grilla);
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

    // No escribe nada: es lo que muestra la previsualización y lo que después recorre el alta.
    private PlanProgramacion planDe(Programacion grilla, Pelicula pelicula, LocalDate tope) {
        List<LocalDateTime> pendientes = grilla.horariosSinGenerar(tope).stream()
                // R20: lo que ya pasó no se programa ni se lista, así la previsualización muestra
                // exactamente lo que el alta va a crear. No es un choque: no va a salteadas.
                .filter(inicio -> !funciones.yaPaso(inicio))
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
            plan.add(new FuncionPlanificada(inicio, false, null));
        }
        return new PlanProgramacion(grilla, plan);
    }

    // Crea lo que el plan dejó programable, en su orden. programar() revisa R3 y R20 otra vez antes de
    // guardar cada una, y la primera que rechace corta la generación sin marcar la grilla como avanzada.
    private PlanProgramacion generar(Programacion grilla, Pelicula pelicula, LocalDate tope) {
        PlanProgramacion plan = planDe(grilla, pelicula, tope);
        for (FuncionPlanificada funcion : plan.programables()) {
            funciones.programar(grilla.getPeliculaId(), grilla.getSalaId(), funcion.inicio(),
                    grilla.getVersion(), grilla.getProyeccion(), grilla.getPrecio(), grilla.getId());
        }
        grilla.marcarGeneradaHasta(tope);
        return plan;
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

    public Programacion desactivar(int id) {
        Programacion grilla = buscarOFallar(id);
        grilla.desactivar();
        return grilla;
    }

    public Programacion activar(int id) {
        Programacion grilla = buscarOFallar(id);
        grilla.activar();
        return grilla;
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
