package ar.uade.cine.controller.programaciones;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.controller.http.Parseo;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.dto.programaciones.IndicadoresGrillaDTO;
import ar.uade.cine.dto.programaciones.PaseSugeridoDTO;
import ar.uade.cine.dto.programaciones.PedidoGrillaDTO;
import ar.uade.cine.dto.programaciones.PeliculaElegidaDTO;
import ar.uade.cine.dto.programaciones.PropuestaGrillaDTO;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.service.programaciones.CriteriosGrilla;
import ar.uade.cine.service.programaciones.PlanificadorGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Grilla automática", description = "El armado de una semana entera de una sola vez")
@RestController
@RequiredArgsConstructor
public class GrillaController {

    private final PlanificadorGrilla planificador;
    private final Reloj reloj;

    @Operation(summary = "Proponer una semana entera de funciones, sin escribir nada")
    @PostMapping("/api/grilla/propuesta")
    public PropuestaGrillaDTO proponer(@Valid @RequestBody PedidoGrillaDTO pedido) {
        return propuesta(planificador.proponer(criterios(pedido)), false);
    }

    @Operation(summary = "Aplicar la propuesta: crea todas las funciones")
    @PostMapping("/api/grilla")
    @ResponseStatus(HttpStatus.CREATED)
    public PropuestaGrillaDTO aplicar(@Valid @RequestBody PedidoGrillaDTO pedido) {
        return propuesta(planificador.aplicar(criterios(pedido)), true);
    }

    // El precio no tiene default: es una decisión comercial del cine. Acá solo se lee el pedido;
    // lo que falte lo completa CriteriosGrilla, salvo el día de hoy, que sale del reloj.
    private CriteriosGrilla criterios(PedidoGrillaDTO pedido) {
        LocalDate desde = Parseo.diaOpcional(pedido.desde(), "la fecha de inicio");
        LocalTime apertura = Parseo.horaOpcional(pedido.apertura(), "la hora de apertura");
        LocalTime cierre = Parseo.horaOpcional(pedido.cierre(), "la hora de cierre");
        Version version = pedido.idioma() == null
                ? null : Parseo.constante(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = pedido.proyeccion() == null
                ? null : Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección");
        return CriteriosGrilla.completando(desde == null ? reloj.hoy() : desde, pedido.dias(), apertura,
                cierre, pedido.cuantasPeliculas(), Dinero.de(pedido.precio()), version, proyeccion);
    }

    private static PropuestaGrillaDTO propuesta(PropuestaGrilla propuesta, boolean creadas) {
        Map<Integer, Integer> pasesPorPelicula = new LinkedHashMap<>();
        propuesta.pases().forEach(p -> pasesPorPelicula.merge(p.peliculaId(), 1, Integer::sum));

        return new PropuestaGrillaDTO(
                propuesta.elenco().stream().map(p -> elegida(p, pasesPorPelicula)).toList(),
                propuesta.pases().stream().map(GrillaController::pase).toList(),
                indicadores(propuesta),
                creadas ? propuesta.pases().size() : 0);
    }

    private static PeliculaElegidaDTO elegida(Pelicula pelicula, Map<Integer, Integer> pases) {
        return new PeliculaElegidaDTO(pelicula.getId(), pelicula.getTitulo(), pelicula.getPuntaje(),
                pelicula.getDuracionMinutos(),
                pelicula.getGeneros().stream().map(Enum::name).toList(),
                pases.getOrDefault(pelicula.getId(), 0));
    }

    private static PaseSugeridoDTO pase(PaseSugerido pase) {
        return new PaseSugeridoDTO(pase.peliculaId(), pase.titulo(), pase.salaId(), pase.sala(),
                Fechas.texto(pase.inicio()), pase.duracionMinutos());
    }

    private static IndicadoresGrillaDTO indicadores(PropuestaGrilla propuesta) {
        var medidas = propuesta.indicadores();
        Map<String, Integer> porGenero = new LinkedHashMap<>();
        medidas.pasesPorGenero().forEach((genero, cuantos) -> porGenero.put(genero.name(), cuantos));
        return new IndicadoresGrillaDTO(medidas.minutosProgramados(), medidas.minutosDisponibles(),
                medidas.ocupacion(), medidas.puntajePromedio(), medidas.generosCubiertos(),
                medidas.generosTotales(), porGenero);
    }
}
