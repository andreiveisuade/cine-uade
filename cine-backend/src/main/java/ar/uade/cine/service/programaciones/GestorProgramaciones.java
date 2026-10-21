package ar.uade.cine.service.programaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.programaciones.ProgramacionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.service.programaciones.PlanProgramacion.FuncionPlanificada;
import ar.uade.cine.service.funciones.AgendaDeSala;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.infrastructure.reloj.Reloj;

// Alta, baja y extensión de grillas; genera cada función por GestorFunciones para no reescribir R3 ni R20.
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GestorProgramaciones {

    private final ProgramacionRepository programacionRepository;
    private final FuncionRepository funcionRepository;
    private final SalaRepository salaRepository;
    private final GestorFunciones funciones;
    private final Reloj reloj;

    @Transactional(readOnly = true)
    public PlanProgramacion previsualizar(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        return planDe(grilla, grilla.getPelicula(), grilla.topePara(reloj.hoy()));
    }

    // Recalcula R3: desde la previsualización otro pudo programar en la sala.
    public PlanProgramacion crear(DatosGrilla datos) {
        Programacion grilla = armar(datos);
        programacionRepository.save(grilla);
        return generar(grilla, grilla.getPelicula(), grilla.topePara(reloj.hoy()));
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
                // La película se vuelve a pedir, y no por grilla.getPelicula(): fuera de la transacción ese
                // proxy no se puede leer, y desde el alta la pudieron descartar.
                Pelicula pelicula = funciones.peliculaProgramable(grilla.getPeliculaId());
                int nuevas = generar(grilla, pelicula, grilla.topePara(hoy)).programables().size();
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
        LocalDateTime ahora = reloj.ahora();
        List<LocalDateTime> pendientes = grilla.horariosSinGenerar(tope).stream()
                // R20: lo que ya pasó no se programa ni se lista, así la previsualización muestra
                // exactamente lo que el alta va a crear. No es un choque: no va a salteadas.
                .filter(inicio -> !Funcion.yaPaso(inicio, ahora))
                .toList();
        // Una sola lectura de la sala para todo el rango, y no una por horario. Las funciones
        // que esta misma grilla va creando no están en la agenda: caen una por día, así que
        // solo chocarían con una película de más de un día, y a esa la frena programar(),
        // que vuelve a mirar R3 antes de guardar.
        int duracion = pelicula.getDuracionMinutos();
        // Null Object: sin pendientes no se lee la sala.
        AgendaDeSala agenda = pendientes.isEmpty() ? AgendaDeSala.vacia() : funciones.agendaDe(grilla.getSalaId(),
                pendientes.get(0), pendientes.get(pendientes.size() - 1).plusMinutes(duracion));
        List<FuncionPlanificada> plan = pendientes.stream()
                .map(inicio -> agenda.chocaCon(inicio, inicio.plusMinutes(duracion))
                        .map(choque -> FuncionPlanificada.salteadaPor(inicio, choque))
                        .orElseGet(() -> FuncionPlanificada.programable(inicio)))
                .toList();
        return new PlanProgramacion(grilla, plan);
    }

    // Crea lo que el plan dejó programable, en su orden. programar() revisa R3 y R20 otra vez antes de
    // guardar cada una, y la primera que rechace corta la generación sin marcar la grilla como avanzada.
    private PlanProgramacion generar(Programacion grilla, Pelicula pelicula, LocalDate tope) {
        PlanProgramacion plan = planDe(grilla, pelicula, tope);
        for (FuncionPlanificada funcion : plan.programables()) {
            funciones.programar(grilla.getPeliculaId(), grilla.getSalaId(), funcion.inicio(),
                    grilla.getFormato(), grilla.getId());
        }
        grilla.marcarGeneradaHasta(tope);
        return plan;
    }

    // Primero lo que se busca, con su 404, como en el alta de una función suelta: la película confirmada y
    // la sala, que la programación necesita para R8. Después la programación valida sus datos, y al
    // final lo que depende del reloj.
    private Programacion armar(DatosGrilla datos) {
        Programacion grilla = new Programacion(funciones.peliculaProgramable(datos.peliculaId()),
                salaRepository.exigir(datos.salaId(), "la sala"), datos.desde(), datos.hasta(),
                datos.horaInicio(), datos.diasSemana(), datos.formato());
        grilla.exigirGenerableA(reloj.ahora());
        return grilla;
    }

    public Programacion desactivar(int id) {
        Programacion grilla = programacionRepository.exigir(id, "la programación");
        grilla.desactivar();
        return grilla;
    }

    public Programacion activar(int id) {
        Programacion grilla = programacionRepository.exigir(id, "la programación");
        grilla.activar();
        return grilla;
    }

    @Transactional(readOnly = true)
    public List<Programacion> buscar(Integer peliculaId, Integer salaId, Boolean activa) {
        return programacionRepository.buscar(peliculaId, salaId, activa);
    }

    @Transactional(readOnly = true)
    public Programacion obtener(int id) {
        return programacionRepository.exigir(id, "la programación");
    }

    @Transactional(readOnly = true)
    public List<Funcion> funcionesDe(int id) {
        return funcionRepository.findByProgramacionId(id);
    }
}
