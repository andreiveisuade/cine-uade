package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCartelera;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Una grilla se previsualiza y recién entonces se confirma: qué manda cada paso, qué informa y cuándo se invalida.
class PantallaProgramacionesTest {

    private static final String GRILLA_ABIERTA = "{\"id\":4,\"peliculaId\":7,\"salaId\":1,\"desde\":\"2026-11-01\","
            + "\"generadaHasta\":\"2026-11-30\",\"horaInicio\":\"20:30:00\",\"diasSemana\":[\"FRIDAY\",\"SATURDAY\"],"
            + "\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\",\"precio\":5000.0,\"activa\":true}";
    private static final String GRILLA_CERRADA = "{\"id\":5,\"peliculaId\":9,\"salaId\":1,\"desde\":\"2026-10-01\","
            + "\"hasta\":\"2026-10-31\",\"horaInicio\":\"18:00:00\",\"idioma\":\"DOBLADA\",\"proyeccion\":\"DOS_D\","
            + "\"precio\":4000.0,\"activa\":false}";
    private static final String PLAN = "{\"funciones\":[{\"inicio\":\"2026-11-06T20:30:00\",\"choca\":false},"
            + "{\"inicio\":\"2026-11-07T20:30:00\",\"choca\":true,\"motivo\":\"se pisa con Coco\"}],"
            + "\"generadas\":1,\"salteadas\":1}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/peliculas", 200, "[{\"id\":7,\"titulo\":\"Matrix\",\"duracionMinutos\":136,"
                + "\"generos\":[],\"clasificacion\":\"ATP\",\"enCartelera\":true}]");
        servidor.responder("GET /api/salas", 200, "[{\"id\":1,\"nombre\":\"Sala 1\",\"tipo\":\"DOS_D\","
                + "\"butacasPorFila\":[10],\"filas\":1,\"capacidadSala\":10,\"minutosLimpieza\":15}]");
        servidor.responder("GET /api/idiomas", 200, "[\"DOBLADA\"]");
        servidor.responder("GET /api/proyecciones", 200, "[\"DOS_D\"]");
        servidor.responder("GET /api/programaciones", 200, "[" + GRILLA_ABIERTA + "," + GRILLA_CERRADA + "]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaProgramaciones(new ApiCartelera(http), new ApiCatalogos(http),
                new ApiProgramaciones(http), new ApiSalas(http)));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void listaCadaGrillaConSuRango() {
        encargado.esperarTexto("2 grillas");

        assertEquals("Matrix", encargado.celda(0, "Película"));
        assertEquals("2026-11-01 en adelante · 20:30 (generada hasta 2026-11-30)", encargado.celda(0, "Cuándo"));
        assertEquals("Vie, Sáb", encargado.celda(0, "Días"));
        assertEquals("Película 9", encargado.celda(1, "Película"), "una película que ya no está se nombra por id");
        assertEquals("2026-10-01 al 2026-10-31 · 18:00", encargado.celda(1, "Cuándo"));
        assertEquals("Dada de baja", encargado.celda(1, "Estado"));
    }

    @Test
    void elegirUnaGrillaMuestraLasFuncionesQueGenero() {
        servidor.responder("GET /api/programaciones/4", 200, GRILLA_ABIERTA.replace("}",
                ",\"funciones\":[{\"id\":10,\"inicio\":\"2026-11-06T20:30:00\"}]}").replace("]}]}", "]}]}"));
        servidor.responder("GET /api/programaciones/5", 200, GRILLA_CERRADA);

        encargado.elegirFila(0);
        encargado.esperarArea("Funciones de la grilla 4 (1):");

        encargado.elegirFila(1);
        encargado.esperarArea("Esta grilla no generó ninguna función");
    }

    @Test
    void previsualizaYRecienDespuesConfirma() {
        servidor.responder("POST /api/programaciones/previsualizacion", 200, PLAN);
        servidor.responder("POST /api/programaciones", 201, PLAN);
        assertFalse(encargado.habilitado("Confirmar"));
        encargado.fecha("Desde *", LocalDate.of(2026, 11, 6))
                .fecha("Hasta", LocalDate.of(2026, 11, 7))
                .hora("Hora", LocalTime.of(20, 30))
                .tildar("Vie")
                .tildar("Sáb")
                .clic("Previsualizar");

        encargado.esperarArea("Se van a generar 1 funciones, 1 se saltean");
        assertTrue(encargado.areas().stream().anyMatch(a -> a.contains("✗ se pisa con Coco")));
        assertEquals("{\"peliculaId\":7,\"salaId\":1,\"desde\":\"2026-11-06\",\"hasta\":\"2026-11-07\","
                        + "\"horaInicio\":\"20:30\",\"diasSemana\":[\"FRIDAY\",\"SATURDAY\"],\"idioma\":\"DOBLADA\","
                        + "\"proyeccion\":\"DOS_D\",\"precio\":5000.0}",
                servidor.ultimo("POST /api/programaciones/previsualizacion").cuerpo());

        encargado.clic("Confirmar");

        encargado.esperarTexto("Grilla creada: 1 funciones, 1 salteadas");
        encargado.esperarArea("Se generaron 1 funciones, 1 se saltearon");
        assertEquals(servidor.ultimo("POST /api/programaciones/previsualizacion").cuerpo(),
                servidor.ultimo("POST /api/programaciones").cuerpo(), "se confirma lo mismo que se vio");
    }

    @Test
    void cambiarUnCampoDespuesDePrevisualizarObligaAPrevisualizarDeNuevo() {
        servidor.responder("POST /api/programaciones/previsualizacion", 200, PLAN);
        encargado.clic("Previsualizar");
        encargado.esperar(() -> encargado.habilitado("Confirmar"));

        encargado.tipear("Precio base", "6000");

        assertFalse(encargado.habilitado("Confirmar"));
        assertEquals("", encargado.areas().get(0));
    }

    @Test
    void sinPrecioNoSePrevisualiza() {
        encargado.tipear("Precio base", "").clic("Previsualizar");

        encargado.esperarTexto("Falta completar «Precio base»");
        assertEquals(0, servidor.veces("POST /api/programaciones/previsualizacion"));
    }

    @Test
    void elRechazoDelRangoSeMuestraTalCual() {
        servidor.responder("POST /api/programaciones/previsualizacion", 400,
                "{\"error\":\"El rango termina antes de empezar\"}");
        encargado.fecha("Hasta", LocalDate.of(2020, 1, 1)).clic("Previsualizar");

        encargado.esperarTexto("El rango termina antes de empezar");
        assertFalse(encargado.habilitado("Confirmar"));
    }
}
