package ar.uade.cine.controller.programaciones;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.dto.programaciones.PedidoGrillaDTO;
import ar.uade.cine.dto.programaciones.PropuestaGrillaVistaDTO;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.programaciones.CriteriosGrilla;
import ar.uade.cine.service.programaciones.PlanificadorGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Rutas de /api/grilla: la semana automática vía PlanificadorGrilla; responde con VistasProgramaciones.
@Tag(name = "Grilla automática", description = "El armado de una semana entera de una sola vez")
@RestController
@RequiredArgsConstructor
public class GrillaController {

    private final PlanificadorGrilla planificador;
    private final VistasProgramaciones vistas;
    private final Reloj reloj;

    @Operation(summary = "Proponer una semana entera de funciones, sin escribir nada")
    @PostMapping("/api/grilla/propuesta")
    public PropuestaGrillaVistaDTO proponer(@Valid @RequestBody PedidoGrillaDTO pedido) {
        return vistas.propuesta(planificador.proponer(criterios(pedido)), 0);
    }

    // Alta sin Location: crea muchas funciones y no un recurso; cada función tiene su propio GET.
    @Operation(summary = "Aplicar la propuesta: crea todas las funciones")
    @PostMapping("/api/grilla")
    @ResponseStatus(HttpStatus.CREATED)
    public PropuestaGrillaVistaDTO aplicar(@Valid @RequestBody PedidoGrillaDTO pedido) {
        PropuestaGrilla aplicada = planificador.aplicar(criterios(pedido));
        return vistas.propuesta(aplicada, aplicada.pases().size());
    }

    // El precio no tiene default: es una decisión comercial del cine. Acá solo se lee el pedido; lo
    // que falte lo completa CriteriosGrilla, con el día de hoy que sale del reloj.
    // Un idioma o una proyección vacíos cuentan como no enviados, igual que las fechas y las horas.
    private CriteriosGrilla criterios(PedidoGrillaDTO pedido) {
        LocalDate desde = Parseo.diaOpcional(pedido.desde(), "la fecha de inicio");
        LocalTime apertura = Parseo.horaOpcional(pedido.apertura(), "la hora de apertura");
        LocalTime cierre = Parseo.horaOpcional(pedido.cierre(), "la hora de cierre");
        Version version = Parseo.constanteOpcional(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = Parseo.constanteOpcional(Proyeccion.class, pedido.proyeccion(), "la proyección");
        return CriteriosGrilla.completando(reloj.hoy(), desde, pedido.dias(), apertura, cierre,
                pedido.cuantasPeliculas(), Dinero.de(pedido.precio()), version, proyeccion);
    }
}
