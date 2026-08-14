package ar.uade.cine.api.rutas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.api.ApiEnMemoria;

/**
 * Pedir cartelera por HTTP, con un importador que no sale a TMDB.
 *
 * <p>Lo que agrega sobre {@code GestorImportacionesTest} es el borde: que el pedido sin
 * cuerpo valga, que el rechazo del gestor salga como 400 con el mensaje intacto, y que las
 * fechas viajen en el formato del contrato.
 */
class RutasImportacionesTest {

    @TempDir
    Path tempDir;

    private ApiEnMemoria api;

    @BeforeEach
    void levantarLaApi() {
        api = new ApiEnMemoria(tempDir);
    }

    @AfterEach
    void bajarLaApi() {
        api.close();
    }

    @Test
    void sinNingunaCorridaElHistorialEstaVacio() {
        ApiEnMemoria.Respuesta respuesta = api.get("/api/importaciones");

        assertEquals(200, respuesta.estado());
        assertEquals(0, respuesta.json().size());
    }

    @Test
    void pedirUnaCorridaDevuelveLoQueTrajo() {
        api.importador().queTraiga(18, 2, 1);

        ApiEnMemoria.Respuesta respuesta = api.post("/api/importaciones", "{\"paginas\":2}");

        assertEquals(201, respuesta.estado());
        assertEquals("TERMINADA", respuesta.json().get("estado").asText());
        assertEquals(18, respuesta.json().get("nuevas").asInt());
        assertEquals(2, respuesta.json().get("salteadas").asInt());
        assertEquals(1, respuesta.json().get("fallidas").asInt());
        assertEquals(2, respuesta.json().get("paginas").asInt());
        assertEquals(2, api.importador().paginasPedidas());
    }

    /** «Traeme cartelera» es un pedido completo: el default de páginas lo pone el gestor. */
    @Test
    void sinCuerpoTambienVale() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/importaciones", "");

        assertEquals(201, respuesta.estado());
        assertEquals(1, respuesta.json().get("paginas").asInt());
    }

    @Test
    void lasFechasViajanEnIsoLocalYLaQueNoPasoEnNull() {
        api.importador().queFalleCon("no responde");

        ApiEnMemoria.Respuesta respuesta = api.post("/api/importaciones", "{}");

        assertTrue(respuesta.json().get("pedidaEn").asText().matches(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"),
                "la fecha tiene que salir como 2026-08-14T15:20:11");
        assertFalse(respuesta.json().get("terminoEn").isNull(),
                "una corrida fallida igual terminó");
    }

    /**
     * El rechazo del gestor sale como 400 con el texto intacto, que es lo que el front
     * muestra tal cual. Es justamente lo que un test de gestor no puede ver.
     */
    @Test
    void pedirCuatroPaginasEs400ConElMensajeDelGestor() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/importaciones", "{\"paginas\":4}");

        assertEquals(400, respuesta.estado());
        assertEquals("Las páginas a importar van de 1 a 3", respuesta.error());
        assertEquals(0, api.importador().corridas());
    }

    /**
     * Que el importador no esté no es un error de la API: la corrida queda registrada como
     * fallida y se responde 201 igual. El motivo se le muestra al encargado.
     */
    @Test
    void siElImportadorNoEstaLaCorridaQuedaFallidaYNoEsUn500() {
        api.importador().queFalleCon("El importador no responde en http://parser:8090");

        ApiEnMemoria.Respuesta respuesta = api.post("/api/importaciones", "{}");

        assertEquals(201, respuesta.estado());
        assertEquals("FALLIDA", respuesta.json().get("estado").asText());
        assertEquals("El importador no responde en http://parser:8090",
                respuesta.json().get("detalle").asText());
    }

    @Test
    void loQueSePidioQuedaEnElHistorial() {
        api.importador().queTraiga(5, 0, 0);
        api.post("/api/importaciones", "{}");

        ApiEnMemoria.Respuesta respuesta = api.get("/api/importaciones");

        assertEquals(1, respuesta.json().size());
        assertEquals(5, respuesta.json().get(0).get("nuevas").asInt());
        assertEquals("TERMINADA", respuesta.json().get(0).get("estado").asText());
    }

    /**
     * El doble clic. Con la espera que el sistema trae de fábrica —un minuto—, el segundo
     * pedido no llega a TMDB: la cartelera no cambió en veinte segundos y cada corrida son
     * sesenta llamadas contra una cuota.
     */
    @Test
    void apretarDosVecesSeguidoNoCorreDosVeces() {
        api.post("/api/importaciones", "{}");

        ApiEnMemoria.Respuesta segunda = api.post("/api/importaciones", "{}");

        assertEquals(400, segunda.estado());
        assertEquals("El importador corrió recién: esperá 60 segundos antes de volver a pedirlo",
                segunda.error());
        assertEquals(1, api.importador().corridas());
    }

    @Test
    void elEstadoDelImportadorSeConsultaAntesDeApretarElBoton() {
        api.importador().queEste(false, "revisá que el contenedor esté levantado");

        ApiEnMemoria.Respuesta respuesta = api.get("/api/importaciones/estado");

        assertEquals(200, respuesta.estado());
        assertFalse(respuesta.json().get("disponible").asBoolean());
        assertEquals("revisá que el contenedor esté levantado",
                respuesta.json().get("detalle").asText());
    }

    /**
     * La ruta del estado va antes que la del listado en el registro, pero además tiene que
     * seguir siendo alcanzable: si Javalin resolviera el listado primero, esto devolvería
     * una lista y el front no tendría cómo saber si el importador está.
     */
    @Test
    void elEstadoNoSeLoComeElListado() {
        assertTrue(api.get("/api/importaciones/estado").json().has("disponible"));
    }
}
