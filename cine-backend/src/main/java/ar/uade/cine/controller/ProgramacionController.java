package ar.uade.cine.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.NoEncontrado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.dto.programaciones.FuncionGeneradaVistaDTO;
import ar.uade.cine.dto.programaciones.FuncionPlanificadaVistaDTO;
import ar.uade.cine.dto.programaciones.PedidoProgramacionDTO;
import ar.uade.cine.dto.programaciones.PlanVistaDTO;
import ar.uade.cine.dto.programaciones.ProgramacionVistaDTO;
import ar.uade.cine.service.programaciones.DatosGrilla;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.programaciones.PlanProgramacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CU-03b: el ABM de la grilla. {@code /previsualizar} devuelve el mismo informe que el alta
 * sin escribir nada, para ver qué choca antes de confirmar. Arma sus DTO sin un
 * {@code Vistas*} porque una programación no necesita otro gestor para completarse.
 */
@Tag(name = "Programaciones", description = "Las grillas que generan funciones en serie")
@RestController
public class ProgramacionController {

    private final GestorProgramaciones programaciones;

    public ProgramacionController(GestorProgramaciones programaciones) {
        this.programaciones = programaciones;
    }

    @Operation(summary = "Las grillas cargadas, activas y dadas de baja")
    @GetMapping("/api/programaciones")
    public List<ProgramacionVistaDTO> listar(@RequestParam(required = false) String peliculaId,
                                             @RequestParam(required = false) String salaId,
                                             @RequestParam(required = false) String activa) {
        return programaciones.buscar(
                        Parseo.numeroOpcional(peliculaId, "la película"),
                        Parseo.numeroOpcional(salaId, "la sala"),
                        Parseo.booleanOpcional(activa, "activa"))
                .stream()
                .map(p -> programacion(p, null))
                .toList();
    }

    @Operation(summary = "Una grilla con las funciones que generó")
    @GetMapping("/api/programaciones/{id}")
    public ProgramacionVistaDTO detalle(@PathVariable int id) {
        Programacion grilla = buscar(id);
        return programacion(grilla, programaciones.funcionesDe(grilla.getId()));
    }

    /** Una consulta como POST: lleva el mismo cuerpo que el alta, ilegible en la query. */
    @Operation(summary = "Ver qué funciones saldrían y cuáles chocan, sin escribir nada")
    @PostMapping("/api/programaciones/previsualizar")
    public PlanVistaDTO previsualizar(@RequestBody PedidoProgramacionDTO pedido) {
        return plan(aplicar(pedido, false));
    }

    @Operation(summary = "Crear la grilla y generar sus funciones")
    @PostMapping("/api/programaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanVistaDTO crear(@RequestBody PedidoProgramacionDTO pedido) {
        return plan(aplicar(pedido, true));
    }

    /** No hay DELETE: la grilla sigue explicando las funciones que generó. */
    @Operation(summary = "Dar de baja una grilla: deja de generar funciones nuevas")
    @PostMapping("/api/programaciones/{id}/baja")
    public ProgramacionVistaDTO desactivar(@PathVariable int id) {
        buscar(id);
        programaciones.desactivar(id);
        return programacion(buscar(id), null);
    }

    @Operation(summary = "Volver a activar una grilla")
    @PostMapping("/api/programaciones/{id}/alta")
    public ProgramacionVistaDTO activar(@PathVariable int id) {
        buscar(id);
        programaciones.activar(id);
        return programacion(buscar(id), null);
    }

    /** Una sola lectura del pedido para los dos caminos, así el informe siempre predice el alta. */
    private PlanProgramacion aplicar(PedidoProgramacionDTO pedido, boolean persistir) {
        int peliculaId = pedido.peliculaId() == null ? 0 : pedido.peliculaId();
        int salaId = pedido.salaId() == null ? 0 : pedido.salaId();
        LocalDate desde = Parseo.dia(pedido.desde(), "la fecha de inicio");
        // Sin fecha de fin la grilla es abierta: corre hasta que la den de baja.
        LocalDate hasta = pedido.hasta() == null || pedido.hasta().isBlank()
                ? null : Parseo.dia(pedido.hasta(), "la fecha de fin");
        var hora = Parseo.hora(pedido.horaInicio(), "la hora de la función");
        Set<DayOfWeek> dias = Set.copyOf(
                Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "los días de la semana"));
        Version version = Parseo.constante(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección");
        Dinero precio = Dinero.de(pedido.precio() == null ? 0 : pedido.precio());

        DatosGrilla datos = new DatosGrilla(peliculaId, salaId, desde, hasta, hora, dias, version,
                proyeccion, precio);
        return persistir ? programaciones.crear(datos) : programaciones.previsualizar(datos);
    }

    private static PlanVistaDTO plan(PlanProgramacion plan) {
        return new PlanVistaDTO(
                programacion(plan.programacion(), null),
                plan.funciones().stream()
                        .map(f -> new FuncionPlanificadaVistaDTO(Fechas.texto(f.inicio()), f.choca(), f.motivo()))
                        .toList(),
                plan.programables().size(),
                plan.salteadas().size());
    }

    /** {@code generadas} en null deja el campo afuera: es el listado, no el detalle. */
    private static ProgramacionVistaDTO programacion(Programacion p, List<Funcion> generadas) {
        return new ProgramacionVistaDTO(p.getId(), p.getPeliculaId(), p.getSalaId(),
                p.getDesde().toString(), texto(p.getHasta()), texto(p.getGeneradaHasta()),
                p.getHoraInicio().toString(),
                p.getDiasSemana().stream().map(Enum::name).toList(),
                p.getVersion().name(), p.getProyeccion().name(), p.getPrecio().aPesos(), p.estaActiva(),
                generadas == null ? null : generadas.stream()
                        .map(f -> new FuncionGeneradaVistaDTO(f.getId(), Fechas.texto(f.getInicio())))
                        .toList());
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private Programacion buscar(int id) {
        return programaciones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la programación " + id));
    }
}
