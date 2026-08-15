package ar.uade.cine.infraestructura.importador.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.infraestructura.importador.ImportadorCartelera;
import ar.uade.cine.infraestructura.importador.ImportadorError;

/**
 * El cliente del importador, contra un servidor de mentira que contesta lo que el test
 * quiera.
 *
 * <p>Es la única pieza del circuito que los tests de ruta no ven: {@code ApiEnMemoria} usa
 * un {@code ImportadorDePrueba} y nunca pasa por HTTP. Lo que se prueba acá es la
 * traducción —cómo se lee el resumen, cómo se convierte un error del importador en un
 * mensaje que se le puede mostrar al encargado— y es justo donde un cambio silencioso
 * rompería la pantalla sin romper ningún otro test.
 *
 * <p>Con {@code com.sun.net.httpserver} del JDK y no con un mock de {@code HttpClient}:
 * así se ejerce el pedido de verdad, serialización y códigos de estado incluidos. Puerto
 * 0, que lo elige el sistema operativo, por lo mismo que {@code ApiEnMemoria}.
 */
class ImportadorHttpTest {

    private HttpServer servidor;

    @AfterEach
    void bajarElServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void leeElResumenDeUnaCorrida() {
        ImportadorCartelera importador = importadorContra("/importar", 200, """
                {"nuevas": 18, "salteadas": 2, "fallidas": 1, "segundos": 11.3,
                 "detalle": ["+ Matrix", "– Limbus: sin duración en TMDB"]}""");

        ImportadorCartelera.Resumen resumen = importador.importar(1);

        assertEquals(18, resumen.nuevas());
        assertEquals(2, resumen.salteadas());
        assertEquals(1, resumen.fallidas());
        assertEquals(11.3, resumen.segundos());
        // Las líneas del log llegan como lista y se guardan como texto: es para leer.
        assertEquals("+ Matrix\n– Limbus: sin duración en TMDB", resumen.detalle());
    }

    @Test
    void lasPaginasViajanEnElCuerpo() {
        StringBuilder recibido = new StringBuilder();
        servidor = levantar("/importar", intercambio -> {
            recibido.append(new String(intercambio.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            responder(intercambio, 200, "{\"nuevas\":0,\"salteadas\":0,\"fallidas\":0}");
        });

        new ImportadorHttp(direccion()).importar(3);

        assertTrue(recibido.toString().contains("\"paginas\": 3"), recibido.toString());
    }

    @Test
    void unaCorridaSinDetalleNoInventaTexto() {
        ImportadorCartelera importador = importadorContra("/importar", 200,
                "{\"nuevas\":0,\"salteadas\":20,\"fallidas\":0,\"segundos\":2.5,\"detalle\":[]}");

        assertNull(importador.importar(1).detalle());
    }

    /**
     * El importador manda el motivo ya redactado —le falta el token, ya está corriendo— y
     * ese texto termina en la pantalla del encargado. Que salga intacto es el contrato.
     */
    @Test
    void elErrorDelImportadorSePropagaConSuMensaje() {
        ImportadorCartelera importador = importadorContra("/importar", 503,
                "{\"error\":\"Falta el token de TMDB\"}");

        ImportadorError error = assertThrows(ImportadorError.class, () -> importador.importar(1));

        assertEquals("Falta el token de TMDB", error.getMessage());
    }

    @Test
    void unErrorSinMensajeAlMenosDiceElCodigo() {
        ImportadorCartelera importador = importadorContra("/importar", 500, "{}");

        assertEquals("El importador respondió 500",
                assertThrows(ImportadorError.class, () -> importador.importar(1)).getMessage());
    }

    /**
     * El caso más probable de todos: nadie levantó el contenedor. El mensaje tiene que
     * decir qué hacer, no "connection refused".
     */
    @Test
    void siNoHayNadieDelOtroLadoElMensajeDiceQueHacer() {
        ImportadorCartelera importador = new ImportadorHttp("http://localhost:1");

        ImportadorError error = assertThrows(ImportadorError.class, () -> importador.importar(1));

        assertEquals("El importador no responde en http://localhost:1: revisá que el "
                + "contenedor esté levantado", error.getMessage());
    }

    @Test
    void consultarNoTiraCuandoNoHayNadie() {
        ImportadorCartelera.Estado estado = new ImportadorHttp("http://localhost:1").consultar();

        assertFalse(estado.disponible());
        assertTrue(estado.detalle().contains("revisá que el contenedor esté levantado"));
    }

    @Test
    void consultarDiceQueEstaListo() {
        ImportadorCartelera importador = importadorContra("/salud", 200,
                "{\"ok\":true,\"tokenConfigurado\":true,\"corriendo\":false}");

        ImportadorCartelera.Estado estado = importador.consultar();

        assertTrue(estado.disponible());
        assertEquals("Listo para traer cartelera", estado.detalle());
    }

    /** Levantado pero sin token no sirve para nada, y conviene saberlo antes de apretar. */
    @Test
    void sinTokenNoEstaDisponible() {
        ImportadorCartelera importador = importadorContra("/salud", 200,
                "{\"ok\":true,\"tokenConfigurado\":false,\"corriendo\":false}");

        ImportadorCartelera.Estado estado = importador.consultar();

        assertFalse(estado.disponible());
        assertTrue(estado.detalle().contains("TMDB_TOKEN"), estado.detalle());
    }

    @Test
    void unaBarraDeMasEnLaUrlNoRompeLaRuta() {
        servidor = levantar("/salud", intercambio ->
                responder(intercambio, 200, "{\"ok\":true,\"tokenConfigurado\":true}"));

        assertTrue(new ImportadorHttp(direccion() + "/").consultar().disponible());
    }

    /* ------------------------------------------------------------- el andamiaje */

    private ImportadorCartelera importadorContra(String ruta, int codigo, String cuerpo) {
        servidor = levantar(ruta, intercambio -> responder(intercambio, codigo, cuerpo));
        return new ImportadorHttp(direccion());
    }

    private static HttpServer levantar(String ruta, Contestar contestar) {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress(0), 0);
            servidor.createContext(ruta, intercambio -> {
                try (intercambio) {
                    contestar.a(intercambio);
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el importador de prueba", e);
        }
    }

    private static void responder(com.sun.net.httpserver.HttpExchange intercambio, int codigo,
                                  String cuerpo) throws IOException {
        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", "application/json");
        intercambio.sendResponseHeaders(codigo, datos.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(datos);
        }
    }

    private String direccion() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    @FunctionalInterface
    private interface Contestar {
        void a(com.sun.net.httpserver.HttpExchange intercambio) throws IOException;
    }
}
