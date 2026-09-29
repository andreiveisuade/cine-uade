package ar.uade.cine.controller.programaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Creado;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.dto.comun.PedidoActivacionDTO;
import ar.uade.cine.dto.programaciones.PedidoProgramacionDTO;
import ar.uade.cine.dto.programaciones.PlanVistaDTO;
import ar.uade.cine.dto.programaciones.ProgramacionVistaDTO;
import ar.uade.cine.service.programaciones.DatosGrilla;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/programaciones: previsualiza, crea y (des)activa grillas; responde con VistasProgramaciones.
@Tag(name = "Programaciones", description = "Las grillas que generan funciones en serie")
@RestController
@RequiredArgsConstructor
public class ProgramacionController {

    private final GestorProgramaciones programaciones;
    private final VistasProgramaciones vistas;

    @Operation(summary = "Las grillas cargadas, activas y dadas de baja")
    @GetMapping("/api/programaciones")
    public List<ProgramacionVistaDTO> listar(@RequestParam(required = false) String peliculaId,
                                             @RequestParam(required = false) String salaId,
                                             @RequestParam(required = false) String activa) {
        return programaciones.buscar(
                        Parseo.numeroOpcional(peliculaId, "el id de la película"),
                        Parseo.numeroOpcional(salaId, "el id de la sala"),
                        Parseo.booleanOpcional(activa, "el filtro activa"))
                .stream()
                .map(vistas::programacion)
                .toList();
    }

    @Operation(summary = "Una grilla con las funciones que generó")
    @GetMapping("/api/programaciones/{id}")
    public ProgramacionVistaDTO detalle(@PathVariable int id) {
        Programacion grilla = programaciones.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontrado("No existe la programación " + id));
        return vistas.programacionConFunciones(grilla, programaciones.funcionesDe(id));
    }

    @Operation(summary = "Ver qué funciones saldrían y cuáles chocan, sin escribir nada")
    @PostMapping("/api/programaciones/previsualizacion")
    public PlanVistaDTO previsualizar(@Valid @RequestBody PedidoProgramacionDTO pedido) {
        return vistas.plan(programaciones.previsualizar(datos(pedido)));
    }

    @Operation(summary = "Crear la grilla y generar sus funciones")
    @PostMapping("/api/programaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PlanVistaDTO> crear(@Valid @RequestBody PedidoProgramacionDTO pedido) {
        PlanVistaDTO plan = vistas.plan(programaciones.crear(datos(pedido)));
        return Creado.en("/api/programaciones/" + plan.programacion().id(), plan);
    }

    @Operation(summary = "Dar de baja una grilla (deja de generar funciones nuevas), o reactivarla")
    @PatchMapping("/api/programaciones/{id}")
    public ProgramacionVistaDTO cambiarActivacion(@PathVariable int id,
                                                  @Valid @RequestBody PedidoActivacionDTO pedido) {
        Programacion grilla = pedido.activa() ? programaciones.activar(id) : programaciones.desactivar(id);
        return vistas.programacion(grilla);
    }

    private DatosGrilla datos(PedidoProgramacionDTO pedido) {
        int peliculaId = pedido.peliculaId();
        int salaId = pedido.salaId();
        LocalDate desde = Parseo.dia(pedido.desde(), "la fecha de inicio");
        LocalDate hasta = Parseo.diaOpcional(pedido.hasta(), "la fecha de fin");
        var hora = Parseo.hora(pedido.horaInicio(), "la hora de la función");
        Set<DayOfWeek> dias = Set.copyOf(
                Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "el día de la semana"));
        Version version = Parseo.constante(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección");
        Dinero precio = Dinero.de(pedido.precio());

        return new DatosGrilla(peliculaId, salaId, desde, hasta, hora, dias, version, proyeccion, precio);
    }
}
