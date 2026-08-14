package ar.uade.cine.api;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
import ar.uade.cine.dto.programaciones.FuncionGeneradaVistaDTO;
import ar.uade.cine.dto.programaciones.FuncionPlanificadaVistaDTO;
import ar.uade.cine.dto.programaciones.PedidoProgramacionDTO;
import ar.uade.cine.dto.programaciones.PlanVistaDTO;
import ar.uade.cine.dto.programaciones.ProgramacionVistaDTO;
import ar.uade.cine.servicio.GestorProgramaciones;
import ar.uade.cine.servicio.PlanProgramacion;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * CU-03b: el ABM de la grilla. Programar quince funciones de a una es el trabajo que esta
 * entidad viene a sacar del medio.
 *
 * <p>Son dos endpoints para un solo alta, y es a propósito: {@code /previsualizar}
 * devuelve exactamente el mismo informe que devolvería el alta, sin escribir nada. El
 * administrador ve qué fechas chocan contra lo que ya hay en esa sala <em>antes</em> de
 * confirmar. Es el mismo par que ya tiene el importador de TMDB con su {@code --simular}.
 *
 * <p>Es la única familia de rutas que arma sus propios DTO en vez de delegar en una clase
 * {@code Vistas*}: una programación se dibuja sola —no necesita preguntarle nada a otro
 * gestor para completarse—, así que darle un ensamblador propio sería sumar una
 * indirección sin ganar nada. Las formas viven igual en
 * {@link ar.uade.cine.dto.programaciones}, con todas las demás.
 */
class RutasProgramaciones {

    /** El contrato pide ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static void registrar(Javalin app, GestorProgramaciones programaciones) {

        // Las grillas dadas de baja no se borran nunca: siguen explicando las funciones
        // que crearon. La lista solo crece, y `activa=true` es la pregunta frecuente.
        app.get("/api/programaciones", ctx ->
                ctx.json(programaciones.buscar(
                                Parseo.numeroOpcional(ctx.queryParam("peliculaId"), "la película"),
                                Parseo.numeroOpcional(ctx.queryParam("salaId"), "la sala"),
                                Parseo.booleanOpcional(ctx.queryParam("activa"), "activa"))
                        .stream()
                        .map(p -> programacion(p, null))
                        .toList()));

        app.get("/api/programaciones/{id}", ctx -> {
            Programacion grilla = buscar(programaciones, Parseo.id(ctx));
            ctx.json(programacion(grilla, programaciones.funcionesDe(grilla.getId())));
        });

        // Sin efecto: es una consulta escrita como POST porque lleva el mismo cuerpo que
        // el alta, y meter nueve campos en la query string sería ilegible.
        app.post("/api/programaciones/previsualizar", ctx ->
                ctx.json(plan(aplicar(programaciones, ctx.bodyAsClass(PedidoProgramacionDTO.class), false))));

        app.post("/api/programaciones", ctx ->
                ctx.status(HttpStatus.CREATED)
                        .json(plan(aplicar(programaciones, ctx.bodyAsClass(PedidoProgramacionDTO.class), true))));

        // No hay DELETE, por lo mismo que en promociones: una grilla que ya generó
        // funciones con entradas vendidas tiene que seguir existiendo para explicarlas.
        // Dar de baja solo evita que genere nuevas; las ya generadas no se tocan.
        app.post("/api/programaciones/{id}/baja", ctx -> {
            int id = Parseo.id(ctx);
            buscar(programaciones, id);
            programaciones.desactivar(id);
            ctx.json(programacion(buscar(programaciones, id), null));
        });

        app.post("/api/programaciones/{id}/alta", ctx -> {
            int id = Parseo.id(ctx);
            buscar(programaciones, id);
            programaciones.activar(id);
            ctx.json(programacion(buscar(programaciones, id), null));
        });
    }

    /**
     * La misma lectura del pedido para los dos caminos: si previsualizar y aplicar
     * leyeran el cuerpo por su cuenta, un día uno aceptaría algo que el otro rechaza y el
     * informe dejaría de predecir el alta.
     */
    private static PlanProgramacion aplicar(GestorProgramaciones programaciones,
                                            PedidoProgramacionDTO pedido, boolean persistir) {
        int peliculaId = pedido.peliculaId() == null ? 0 : pedido.peliculaId();
        int salaId = pedido.salaId() == null ? 0 : pedido.salaId();
        var desde = Parseo.dia(pedido.desde(), "la fecha de inicio");
        // Sin fecha de fin la grilla es abierta: corre hasta que la den de baja. Por eso
        // no se exige, a diferencia de desde.
        var hasta = pedido.hasta() == null || pedido.hasta().isBlank()
                ? null : Parseo.dia(pedido.hasta(), "la fecha de fin");
        var hora = Parseo.hora(pedido.horaInicio(), "la hora de la función");
        Set<DayOfWeek> dias = Set.copyOf(
                Parseo.constantes(DayOfWeek.class, pedido.diasSemana(), "los días de la semana"));
        Version version = Parseo.constante(Version.class, pedido.idioma(), "el idioma");
        Proyeccion proyeccion = Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección");
        double precio = pedido.precio() == null ? 0 : pedido.precio();

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
                p.getVersion().name(), p.getProyeccion().name(), p.getPrecio(), p.estaActiva(),
                generadas == null ? null : generadas.stream()
                        .map(f -> new FuncionGeneradaVistaDTO(f.getId(), fecha(f.getInicio())))
                        .toList());
    }

    /** Las fechas que admiten null viajan como null, no como cadena vacía. */
    private static String texto(java.time.LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static String fecha(LocalDateTime momento) {
        return momento.format(ISO);
    }

    private static Programacion buscar(GestorProgramaciones programaciones, int id) {
        return programaciones.buscar(id)
                .orElseThrow(() -> new NoEncontrado("No existe la programación " + id));
    }
}
