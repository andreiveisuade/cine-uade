package ar.uade.cine.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dto.grilla.IndicadoresGrillaDTO;
import ar.uade.cine.dto.grilla.PaseSugeridoDTO;
import ar.uade.cine.dto.grilla.PedidoGrillaDTO;
import ar.uade.cine.dto.grilla.PeliculaElegidaDTO;
import ar.uade.cine.dto.grilla.PropuestaGrillaDTO;
import ar.uade.cine.servicio.CriteriosGrilla;
import ar.uade.cine.servicio.PlanificadorGrilla;
import ar.uade.cine.servicio.PropuestaGrilla;
import ar.uade.cine.servicio.PropuestaGrilla.PaseSugerido;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * El armado automático de la grilla semanal.
 *
 * <p>Dos endpoints para una sola operación, igual que las programaciones: {@code /propuesta}
 * devuelve exactamente lo mismo que devolvería el alta, sin escribir. El encargado puede
 * probar seis títulos contra diez, comparar los indicadores y recién ahí confirmar. Como el
 * planificador es determinista, lo que ve es lo que se va a crear.
 *
 * <p>Arma sus propios DTO adentro de la ruta y no en una clase {@code Vistas*}, por lo
 * mismo que {@code RutasProgramaciones}: la propuesta llega completa desde el servicio y no
 * necesita preguntarle nada a ningún gestor para dibujarse.
 */
class RutasGrilla {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static void registrar(Javalin app, PlanificadorGrilla planificador) {

        // Sin efecto: es una consulta escrita como POST porque lleva el mismo cuerpo que
        // el alta, y meter ocho campos en la query string sería ilegible.
        app.post("/api/grilla/propuesta", ctx ->
                ctx.json(propuesta(planificador.proponer(criterios(ctx.bodyAsClass(PedidoGrillaDTO.class))), false)));

        app.post("/api/grilla", ctx ->
                ctx.status(HttpStatus.CREATED)
                        .json(propuesta(planificador.aplicar(criterios(ctx.bodyAsClass(PedidoGrillaDTO.class))), true)));
    }

    /**
     * Los defaults viven acá y no en el planificador porque son una comodidad de la
     * pantalla, no una regla: un cine que abra a las 10 solo tiene que mandar el campo.
     *
     * <p>El precio es la excepción y no tiene default a propósito. Los otros siete son
     * convenciones razonables —una semana, de 14 a 24, ocho títulos—, pero cuánto sale la
     * entrada es una decisión comercial que el sistema no puede tomar por el cine: si
     * inventara un número y el encargado no lo mirara, se venderían entradas a un precio
     * que no decidió nadie. Por eso falta se avisa acá, con el mismo criterio que usa
     * {@code RutasCandy} con la disponibilidad: comprobar que un campo obligatorio del
     * pedido esté presente es traducción, no una regla de negocio.
     */
    private static CriteriosGrilla criterios(PedidoGrillaDTO pedido) {
        if (pedido.precio() == null) {
            throw new IllegalArgumentException("Falta el precio de las funciones");
        }
        LocalDate desde = pedido.desde() == null || pedido.desde().isBlank()
                ? LocalDate.now()
                : Parseo.dia(pedido.desde(), "la fecha de inicio");
        CriteriosGrilla base = CriteriosGrilla.deUnaSemana(desde,
                pedido.cuantasPeliculas() == null ? 8 : pedido.cuantasPeliculas(),
                pedido.precio());

        return new CriteriosGrilla(desde,
                pedido.dias() == null ? base.dias() : pedido.dias(),
                hora(pedido.apertura(), base.apertura(), "la hora de apertura"),
                hora(pedido.cierre(), base.cierre(), "la hora de cierre"),
                base.cuantasPeliculas(), base.precio(),
                pedido.idioma() == null
                        ? base.version() : Parseo.constante(Version.class, pedido.idioma(), "el idioma"),
                pedido.proyeccion() == null
                        ? base.proyeccion() : Parseo.constante(Proyeccion.class, pedido.proyeccion(), "la proyección"));
    }

    private static LocalTime hora(String valor, LocalTime porDefecto, String queEs) {
        return valor == null || valor.isBlank() ? porDefecto : Parseo.hora(valor, queEs);
    }

    /**
     * @param creadas si las funciones se escribieron de verdad. El front necesita
     *                distinguir "así quedaría" de "así quedó", y contar los pases no
     *                alcanza: son el mismo número en los dos casos
     */
    private static PropuestaGrillaDTO propuesta(PropuestaGrilla propuesta, boolean creadas) {
        Map<Integer, Integer> pasesPorPelicula = new LinkedHashMap<>();
        propuesta.pases().forEach(p -> pasesPorPelicula.merge(p.peliculaId(), 1, Integer::sum));

        return new PropuestaGrillaDTO(
                propuesta.elenco().stream().map(p -> elegida(p, pasesPorPelicula)).toList(),
                propuesta.pases().stream().map(RutasGrilla::pase).toList(),
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
                pase.inicio().format(ISO), pase.duracionMinutos());
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
