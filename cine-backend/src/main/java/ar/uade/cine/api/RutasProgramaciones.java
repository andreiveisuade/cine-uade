package ar.uade.cine.api;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.programaciones.Programacion;
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
 * <p>Las formas del JSON viven acá adentro y no en {@code Vistas}: una programación se
 * dibuja sola —no necesita preguntarle nada a otro gestor para completarse—, así que
 * llevarla al armador central sería sumarle una dependencia sin ganar nada.
 */
class RutasProgramaciones {

    /** El contrato pide ISO local sin zona, con los segundos siempre presentes. */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * El mismo pedido para previsualizar y para dar de alta: son la misma operación, y
     * mandarlos con formas distintas invitaría a que dejaran de serlo.
     *
     * <p>{@code diasSemana} ausente o vacío significa todos los días del rango, no
     * ninguno — igual que en las promociones.
     */
    record PedidoProgramacion(Integer peliculaId, Integer salaId, String desde, String hasta,
                              String horaInicio, List<String> diasSemana, String idioma,
                              String proyeccion, Double precio) {
    }

    /** {@code funciones} solo viaja en el detalle: el listado no arrastra lo que generó. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ProgramacionVista(int id, int peliculaId, int salaId, String desde, String hasta,
                             String generadaHasta, String horaInicio, List<String> diasSemana,
                             String idioma, String proyeccion, double precio, boolean activa,
                             List<FuncionGeneradaVista> funciones) {
    }

    record FuncionGeneradaVista(int id, String inicio) {
    }

    /**
     * Una fecha del rango. {@code motivo} explica contra qué choca y viaja en null cuando
     * no hay choque: el administrador tiene que poder leer por qué se saltea cada una.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FuncionPlanificadaVista(String inicio, boolean choca, String motivo) {
    }

    /**
     * El informe. {@code generadas} y {@code salteadas} son cuentas que el front podría
     * hacer solas, pero son justo lo que se muestra arriba de todo —"12 funciones, 3 se
     * saltearon"— y contarlas del lado del servidor evita que cada pantalla las cuente a
     * su manera.
     */
    record PlanVista(ProgramacionVista programacion, List<FuncionPlanificadaVista> funciones,
                     int generadas, int salteadas) {
    }

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
                ctx.json(plan(aplicar(programaciones, ctx.bodyAsClass(PedidoProgramacion.class), false))));

        app.post("/api/programaciones", ctx ->
                ctx.status(HttpStatus.CREATED)
                        .json(plan(aplicar(programaciones, ctx.bodyAsClass(PedidoProgramacion.class), true))));

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
                                            PedidoProgramacion pedido, boolean persistir) {
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

    private static PlanVista plan(PlanProgramacion plan) {
        return new PlanVista(
                programacion(plan.programacion(), null),
                plan.funciones().stream()
                        .map(f -> new FuncionPlanificadaVista(fecha(f.inicio()), f.choca(), f.motivo()))
                        .toList(),
                plan.programables().size(),
                plan.salteadas().size());
    }

    /** {@code generadas} en null deja el campo afuera: es el listado, no el detalle. */
    private static ProgramacionVista programacion(Programacion p, List<Funcion> generadas) {
        return new ProgramacionVista(p.getId(), p.getPeliculaId(), p.getSalaId(),
                p.getDesde().toString(), texto(p.getHasta()), texto(p.getGeneradaHasta()),
                p.getHoraInicio().toString(),
                p.getDiasSemana().stream().map(Enum::name).toList(),
                p.getVersion().name(), p.getProyeccion().name(), p.getPrecio(), p.estaActiva(),
                generadas == null ? null : generadas.stream()
                        .map(f -> new FuncionGeneradaVista(f.getId(), fecha(f.getInicio())))
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
