package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiProgramaciones;
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

// El planificador pide la propuesta, la muestra y crea exactamente esa; lo vacío lo completa el backend.
class PantallaPlanificadorTest {

    private static final String PROPUESTA = "{\"elenco\":[{\"id\":7,\"titulo\":\"Matrix\",\"puntaje\":8.7,"
            + "\"duracionMinutos\":136,\"generos\":[\"ACCION\"],\"pases\":2}],"
            + "\"pases\":[{\"peliculaId\":7,\"titulo\":\"Matrix\",\"salaId\":1,\"sala\":\"Sala 1\","
            + "\"inicio\":\"2026-11-02T14:00:00\",\"duracionMinutos\":136},"
            + "{\"peliculaId\":7,\"titulo\":\"Matrix\",\"salaId\":1,\"sala\":\"Sala 1\","
            + "\"inicio\":\"2026-11-02T17:00:00\",\"duracionMinutos\":136}],"
            + "\"indicadores\":{\"minutosProgramados\":272,\"minutosDisponibles\":600,\"ocupacion\":0.45,"
            + "\"puntajePromedio\":8.7,\"generosCubiertos\":1,\"generosTotales\":9,"
            + "\"pasesPorGenero\":{\"ACCION\":2}},\"funcionesCreadas\":%d}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/idiomas", 200, "[\"DOBLADA\",\"SUBTITULADA\"]");
        servidor.responder("GET /api/proyecciones", 200, "[\"DOS_D\"]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaPlanificador(new ApiCatalogos(http), new ApiProgramaciones(http)));
        encargado.esperar(() -> encargado.opciones("Idioma").size() == 2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void previsualizaMuestraLaSemanaYAplicarCreaLaMisma() {
        servidor.responder("POST /api/grilla/propuesta", 200, PROPUESTA.formatted(0));
        servidor.responder("POST /api/grilla", 201, PROPUESTA.formatted(2));
        encargado.fecha("Desde", LocalDate.of(2026, 11, 2))
                .tipear("Días", "1")
                .hora("Apertura", LocalTime.of(14, 0))
                .hora("Cierre", LocalTime.of(23, 0))
                .tipear("Películas", "3")
                .elegir("Idioma", "Subtitulada")
                .clic("Previsualizar");

        encargado.esperarTexto("Así quedaría la semana: 2 funciones");
        encargado.esperarTexto("Pediste 3 películas y hay 1 confirmadas: confirmá más en Por revisar.");
        encargado.esperarTexto("Elenco (1)");
        assertEquals("{\"desde\":\"2026-11-02\",\"dias\":1,\"apertura\":\"14:00\",\"cierre\":\"23:00\","
                        + "\"cuantasPeliculas\":3,\"precio\":5000.0,\"idioma\":\"SUBTITULADA\",\"proyeccion\":\"DOS_D\"}",
                servidor.ultimo("POST /api/grilla/propuesta").cuerpo());

        encargado.clic("Crear 2 funciones");

        encargado.esperarTexto("Se crearon 2 funciones");
        encargado.esperarTexto("igual que la anterior");
        assertFalse(encargado.habilitado("Aplicada"));
        assertEquals(servidor.ultimo("POST /api/grilla/propuesta").cuerpo(),
                servidor.ultimo("POST /api/grilla").cuerpo());
    }

    @Test
    void sinPasesLoDiceYNoDejaAplicar() {
        servidor.responder("POST /api/grilla/propuesta", 200, "{\"elenco\":[],\"pases\":[],\"indicadores\":"
                + "{\"pasesPorGenero\":{}},\"funcionesCreadas\":0}");

        encargado.clic("Previsualizar");

        encargado.esperarTexto("Con estos criterios no entra ninguna función");
        assertTrue(encargado.hayBoton("Crear 0 funciones"));
        assertFalse(encargado.habilitado("Crear 0 funciones"));
    }

    @Test
    void loVacioNoViajaYLoCompletaElBackend() {
        servidor.responder("POST /api/grilla/propuesta", 200, PROPUESTA.formatted(0));
        encargado.fecha("Desde", null).tipear("Días", "").tipear("Películas", "").clic("Previsualizar");

        encargado.esperarTexto("Así quedaría la semana");
        assertEquals("{\"apertura\":\"14:00\",\"cierre\":\"00:00\",\"precio\":5000.0,\"idioma\":\"DOBLADA\","
                + "\"proyeccion\":\"DOS_D\"}", servidor.ultimo("POST /api/grilla/propuesta").cuerpo());
    }

    @Test
    void cambiarUnCriterioDescartaLaPropuesta() {
        servidor.responder("POST /api/grilla/propuesta", 200, PROPUESTA.formatted(0));
        encargado.clic("Previsualizar");
        encargado.esperarTexto("Así quedaría la semana");

        encargado.tipear("Precio base", "6000");

        assertTrue(encargado.hayBoton("Aplicar"));
        assertFalse(encargado.habilitado("Aplicar"));
        assertFalse(encargado.textos().stream().anyMatch(t -> t.contains("Así quedaría")));
    }

    @Test
    void elRechazoDelBackendQuedaJuntoALosCriterios() {
        servidor.responder("POST /api/grilla/propuesta", 400,
                "{\"error\":\"No hay películas confirmadas para armar la semana\"}");

        encargado.clic("Previsualizar");

        encargado.esperarTexto("No hay películas confirmadas para armar la semana");
        assertTrue(encargado.habilitado("Previsualizar"));
    }
}
