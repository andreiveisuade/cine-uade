package ar.uade.cine.controller.programaciones;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.uade.cine.controller.http.Fechas;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.programaciones.Programacion;
import ar.uade.cine.dto.programaciones.FuncionGeneradaVistaDTO;
import ar.uade.cine.dto.programaciones.FuncionPlanificadaVistaDTO;
import ar.uade.cine.dto.programaciones.IndicadoresGrillaVistaDTO;
import ar.uade.cine.dto.programaciones.PaseSugeridoVistaDTO;
import ar.uade.cine.dto.programaciones.PeliculaElegidaVistaDTO;
import ar.uade.cine.dto.programaciones.PlanVistaDTO;
import ar.uade.cine.dto.programaciones.ProgramacionVistaDTO;
import ar.uade.cine.dto.programaciones.PropuestaGrillaVistaDTO;
import ar.uade.cine.service.programaciones.PlanProgramacion;
import ar.uade.cine.service.programaciones.PropuestaGrilla;
import ar.uade.cine.service.programaciones.IndicadoresGrilla;
import ar.uade.cine.service.programaciones.PropuestaGrilla.PaseSugerido;

// Arma los JSON de programaciones, sus planes y la grilla automática; Assembler de los dos controllers.
@Component
public class VistasProgramaciones {

    public ProgramacionVistaDTO programacion(Programacion p) {
        return armar(p, null);
    }

    public ProgramacionVistaDTO programacionConFunciones(Programacion p, List<Funcion> generadas) {
        return armar(p, generadas.stream()
                .map(f -> new FuncionGeneradaVistaDTO(f.getId(), Fechas.texto(f.getInicio())))
                .toList());
    }

    public PlanVistaDTO plan(PlanProgramacion plan) {
        return new PlanVistaDTO(
                programacion(plan.programacion()),
                plan.funciones().stream()
                        .map(f -> new FuncionPlanificadaVistaDTO(Fechas.texto(f.inicio()), f.choca(), f.motivo()))
                        .toList(),
                plan.programables().size(),
                plan.salteadas().size());
    }

    public PropuestaGrillaVistaDTO propuesta(PropuestaGrilla propuesta, int creadas) {
        return new PropuestaGrillaVistaDTO(
                propuesta.elenco().stream().map(p -> elegida(p, propuesta.pasesDe(p.getId()))).toList(),
                propuesta.pases().stream().map(this::pase).toList(),
                indicadores(propuesta.indicadores()),
                creadas);
    }

    // Sin funciones, el DTO las omite del JSON: el listado no las trae, solo el detalle.
    private ProgramacionVistaDTO armar(Programacion p, List<FuncionGeneradaVistaDTO> funciones) {
        return new ProgramacionVistaDTO(p.getId(), p.getPeliculaId(), p.getSalaId(),
                p.getPeriodo().desde().toString(), texto(p.getPeriodo().hasta()), texto(p.getGeneradaHasta()),
                p.getHoraInicio().toString(),
                p.getDiasSemana().stream().map(Enum::name).toList(),
                p.getVersion().name(), p.getProyeccion().name(), p.getPrecio().aPesos(), p.estaActiva(),
                funciones);
    }

    private PeliculaElegidaVistaDTO elegida(Pelicula pelicula, int pases) {
        return new PeliculaElegidaVistaDTO(pelicula.getId(), pelicula.getTitulo(), pelicula.getPuntaje(),
                pelicula.getDuracionMinutos(),
                pelicula.getGeneros().stream().map(Enum::name).toList(),
                pases);
    }

    private PaseSugeridoVistaDTO pase(PaseSugerido pase) {
        return new PaseSugeridoVistaDTO(pase.peliculaId(), pase.titulo(), pase.salaId(), pase.sala(),
                Fechas.texto(pase.inicio()), pase.duracionMinutos());
    }

    private IndicadoresGrillaVistaDTO indicadores(IndicadoresGrilla medidas) {
        Map<String, Integer> porGenero = new LinkedHashMap<>();
        medidas.pasesPorGenero().forEach((genero, cuantos) -> porGenero.put(genero.name(), cuantos));
        return new IndicadoresGrillaVistaDTO(medidas.minutosProgramados(), medidas.minutosDisponibles(),
                medidas.ocupacion(), medidas.puntajePromedio(), medidas.generosCubiertos(),
                medidas.generosTotales(), porGenero);
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
