package ar.uade.cine.controller.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.DatosGrilla;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.salas.GestorSalas;

class ProgramacionControllerTest extends PruebaDeApi {

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorProgramaciones programaciones;
    @Autowired
    private GestorFunciones funciones;

    @BeforeEach
    void cargarPeliculaYSala() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
    }

    private static String grilla(String desde, String hasta) {
        return "{\"peliculaId\":1,\"salaId\":1,\"desde\":\"" + desde + "\","
                + (hasta == null ? "" : "\"hasta\":\"" + hasta + "\",")
                + "\"horaInicio\":\"20:30\",\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000}";
    }

    @Test
    void crearEs201ConLocationYGeneraUnaFuncionPorDia() {
        Respuesta respuesta = post("/api/programaciones", grilla("2026-09-08", "2026-09-10"));

        assertEquals(201, respuesta.estado());
        assertEquals("/api/programaciones/1", respuesta.cabeceras().getLocation().getPath());
        assertEquals("{\"programacion\":{\"id\":1,\"peliculaId\":1,\"salaId\":1,\"desde\":\"2026-09-08\","
                + "\"hasta\":\"2026-09-10\",\"generadaHasta\":\"2026-09-10\",\"horaInicio\":\"20:30\","
                + "\"diasSemana\":[],"
                + "\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000.0,\"activa\":true},"
                + "\"funciones\":[{\"inicio\":\"2026-09-08T20:30:00\",\"choca\":false},"
                + "{\"inicio\":\"2026-09-09T20:30:00\",\"choca\":false},"
                + "{\"inicio\":\"2026-09-10T20:30:00\",\"choca\":false}],\"generadas\":3,\"salteadas\":0}",
                respuesta.cuerpo());
    }

    // Lo que ya está programado se marca y se saltea; previsualizar no escribe ni la grilla ni sus funciones.
    @Test
    void previsualizarMarcaLosChoquesYNoEscribeNada() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 9, 9, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));

        Respuesta respuesta = post("/api/programaciones/previsualizacion", grilla("2026-09-08", "2026-09-10"));

        assertEquals(200, respuesta.estado());
        assertEquals("[{\"inicio\":\"2026-09-08T20:30:00\",\"choca\":false},"
                + "{\"inicio\":\"2026-09-09T20:30:00\",\"choca\":true,"
                + "\"motivo\":\"la sala ya tiene la función 1 a las 09/09 20:00\"},"
                + "{\"inicio\":\"2026-09-10T20:30:00\",\"choca\":false}]",
                respuesta.json().get("funciones").toString());
        assertEquals(2, respuesta.json().get("generadas").asInt());
        assertEquals(1, respuesta.json().get("salteadas").asInt());
        assertEquals("[]", get("/api/programaciones").cuerpo());
        assertEquals(1, funciones.listar().size());
    }

    // El listado no trae las funciones; el detalle sí.
    @Test
    void elListadoFiltraYElDetalleTraeLasFuncionesGeneradas() {
        post("/api/programaciones", grilla("2026-09-08", "2026-09-09"));
        int baja = post("/api/programaciones", grilla("2026-09-15", "2026-09-15")).json()
                .get("programacion").get("id").asInt();
        patch("/api/programaciones/" + baja, "{\"activa\":false}");

        assertEquals(2, get("/api/programaciones?peliculaId=1&salaId=1").json().size());
        Respuesta activas = get("/api/programaciones?activa=true");
        assertEquals(1, activas.json().size());
        assertFalse(activas.json().get(0).has("funciones"));
        assertEquals(0, get("/api/programaciones?salaId=2").json().size());
        assertEquals("[{\"id\":1,\"inicio\":\"2026-09-08T20:30:00\"},{\"id\":2,\"inicio\":\"2026-09-09T20:30:00\"}]",
                get("/api/programaciones/1").json().get("funciones").toString());
    }

    // Abierta, genera hasta el horizonte (hoy es el 14/08) y el resto sale a medida que avanza.
    @Test
    void unaGrillaSinHastaQuedaAbiertaYNoMandaElCampo() {
        Respuesta respuesta = post("/api/programaciones", grilla("2026-08-20", null));

        assertEquals(201, respuesta.estado());
        assertFalse(respuesta.json().get("programacion").has("hasta"));
        assertEquals("2026-08-28", respuesta.json().get("programacion").get("generadaHasta").asText());
        assertEquals(9, respuesta.json().get("generadas").asInt());
    }

    @Test
    void reactivarDevuelveLaGrillaActivaOtraVez() {
        post("/api/programaciones", grilla("2026-09-08", "2026-09-10"));
        patch("/api/programaciones/1", "{\"activa\":false}");

        Respuesta respuesta = patch("/api/programaciones/1", "{\"activa\":true}");

        assertEquals(200, respuesta.estado());
        assertTrue(respuesta.json().get("activa").asBoolean());
    }

    @Test
    void darDeBajaDevuelveLaGrillaYaInactiva() {
        int id = programaciones.crear(new DatosGrilla(1, 1, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13),
                LocalTime.of(20, 30), Set.of(),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))))
                .programacion().getId();

        Respuesta respuesta = patch("/api/programaciones/" + id, "{\"activa\":false}");

        assertEquals(200, respuesta.estado());
        assertFalse(respuesta.json().get("activa").asBoolean());
        assertFalse(get("/api/programaciones/" + id).json().get("activa").asBoolean());
    }

    // El precio se valida al convertirlo; el rango después, porque R8 necesita la sala ya buscada.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            precio cero y película inexistente,    99, 2026-09-07, 2026-09-13, 0,    400, El precio tiene que ser mayor a cero
            película inexistente y rango al revés, 99, 2026-09-13, 2026-09-07, 5000, 404, No existe la película 99
            # De 2026 a 9999 eran 2,9 millones de funciones en una transacción, y la previsualización se caía.
            un rango de más de un año,             1,  2026-09-07, 9999-12-31, 5000, 400, El rango no puede cubrir más de 366 días
            """)
    void unaPrevisualizacionInvalidaSeRechazaConSuMensaje(String caso, int pelicula, String desde, String hasta,
            String precio, int estado, String mensaje) {
        Respuesta respuesta = post("/api/programaciones/previsualizacion", "{\"peliculaId\":" + pelicula
                + ",\"salaId\":1,\"desde\":\"" + desde + "\",\"hasta\":\"" + hasta + "\",\"horaInicio\":\"20:30\","
                + "\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\",\"precio\":" + precio + "}");

        assertEquals(estado, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    @Test
    void activarUnaGrillaQueNoExisteEs404ConSuMensaje() {
        Respuesta respuesta = patch("/api/programaciones/99", "{\"activa\":true}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la programación 99", respuesta.error());
    }
}
