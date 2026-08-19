package ar.uade.cine.api.controladores;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import ar.uade.cine.api.http.NoEncontrado;
import ar.uade.cine.api.http.Parseo;
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.dto.programaciones.FuncionGeneradaVistaDTO;
import ar.uade.cine.dto.programaciones.FuncionPlanificadaVistaDTO;
import ar.uade.cine.dto.programaciones.PedidoProgramacionDTO;
import ar.uade.cine.dto.programaciones.PlanVistaDTO;
import ar.uade.cine.dto.programaciones.ProgramacionVistaDTO;
import ar.uade.cine.servicio.programaciones.GestorProgramaciones;
import ar.uade.cine.servicio.programaciones.PlanProgramacion;

/**
 * CU-03b: el ABM de la grilla. Programar quince funciones de a una es el trabajo que esta
 * entidad viene a sacar del medio.
 *
 * <p>Son dos endpoints para un solo alta, y es a propósito: {@code /previsualizar} devuelve
 * exactamente el mismo informe que devolvería el alta, sin escribir nada. El administrador
 * ve qué fechas chocan contra lo que ya hay en esa sala <em>antes</em> de confirmar.
 *
 * <p>Es el único controlador que arma sus propios DTO en vez de delegar en una clase
 * {@code Vistas*}: una programación se dibuja sola —no necesita preguntarle nada a otro
 * gestor para completarse—, así que darle un ensamblador propio sería sumar una indirección
 * sin ganar nada. Las formas viven igual en {@link ar.uade.cine.dto.programaciones}, con
 * todas las demás.
 */
@RestController
public class ControladorProgramaciones {

    /** El contrato pide ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final GestorProgramaciones programaciones;

    public ControladorProgramaciones(GestorProgramaciones programaciones) {
        this.programaciones = programaciones;
    }

    /**
     * Las grillas dadas de baja no se borran nunca: siguen explicando las funciones que
     * crearon. La lista solo crece, y {@code activa=true} es la pregunta frecuente.
     */
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

    @GetMapping("/api/programaciones/{id}")
    public ProgramacionVistaDTO detalle(@PathVariable int id) {
        Programacion grilla = buscar(id);
        return programacion(grilla, programaciones.funcionesDe(grilla.getId()));
    }

    /**
     * Sin efecto: es una consulta escrita como POST porque lleva el mismo cuerpo que el
     * alta, y meter nueve campos en la query string sería ilegible.
     */
    @PostMapping("/api/programaciones/previsualizar")
    public PlanVistaDTO previsualizar(@RequestBody PedidoProgramacionDTO pedido) {
        return plan(aplicar(pedido, false));
    }

    @PostMapping("/api/programaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanVistaDTO crear(@RequestBody PedidoProgramacionDTO pedido) {
        return plan(aplicar(pedido, true));
    }

    /**
     * No hay DELETE, por lo mismo que en promociones: una grilla que ya generó funciones con
     * entradas vendidas tiene que seguir existiendo para explicarlas. Dar de baja solo evita
     * que genere nuevas; las ya generadas no se tocan.
     */
    @PostMapping("/api/programaciones/{id}/baja")
    public ProgramacionVistaDTO desactivar(@PathVariable int id) {
        buscar(id);
        programaciones.desactivar(id);
        return programacion(buscar(id), null);
    }

    @PostMapping("/api/programaciones/{id}/alta")
    public ProgramacionVistaDTO activar(@PathVariable int id) {
        buscar(id);
        programaciones.activar(id);
        return programacion(buscar(id), null);
    }

    /**
     * La misma lectura del pedido para los dos caminos: si previsualizar y aplicar leyeran
     * el cuerpo por su cuenta, un día uno aceptaría algo que el otro rechaza y el informe
     * dejaría de predecir el alta.
     */
    private PlanProgramacion aplicar(PedidoProgramacionDTO pedido, boolean persistir) {
        int peliculaId = pedido.peliculaId() == null ? 0 : pedido.peliculaId();
        int salaId = pedido.salaId() == null ? 0 : pedido.salaId();
        LocalDate desde = Parseo.dia(pedido.desde(), "la fecha de inicio");
        // Sin fecha de fin la grilla es abierta: corre hasta que la den de baja. Por eso no
        // se exige, a diferencia de desde.
        LocalDate hasta = pedido.hasta() == null || pedido.hasta().isBlank()
                ? null : Parseo.dia(pedido.hasta(), "la fecha de fin");
        var hora = Parseo.hora(pedido.horaInicio(), "la hora de la función");
        Set<DayOfWeek> dias = Set.copyOf(
                Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "los días de la semana"));
        Version version = Parseo.constante(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección");
        Dinero precio = Dinero.de(pedido.precio() == null ? 0 : pedido.precio());

        return persistir
                ? programaciones.crear(peliculaId, salaId, desde, hasta, hora, dias, version, proyeccion, precio)
                : programaciones.previsualizar(peliculaId, salaId, desde, hasta, hora, dias, version, proyeccion, precio);
    }

    private static PlanVistaDTO plan(PlanProgramacion plan) {
        return new PlanVistaDTO(
                programacion(plan.programacion(), null),
                plan.funciones().stream()
                        .map(f -> new FuncionPlanificadaVistaDTO(fecha(f.inicio()), f.choca(), f.motivo()))
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
                        .map(f -> new FuncionGeneradaVistaDTO(f.getId(), fecha(f.getInicio())))
                        .toList());
    }

    /** Las fechas que admiten null viajan como null, no como cadena vacía. */
    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static String fecha(LocalDateTime momento) {
        return momento.format(ISO);
    }

    private Programacion buscar(int id) {
        return programaciones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la programación " + id));
    }
}
