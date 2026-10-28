package ar.uade.cine.swing.pantallas.produccion;

import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Render y Aiven de mentira: qué estado se ve, qué pasa sin token o con uno vencido, y el despertar en orden.
class PanelProduccionTest {

    private static final String AIVEN = "/v1/project/cine-prueba/service/mysql-prueba";

    private ServidorFalso render;
    private ServidorFalso aiven;
    private Preferences preferencias;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        render = new ServidorFalso();
        aiven = new ServidorFalso();
        preferencias = Preferences.userRoot().node("/ar/uade/cine/swing/test-" + UUID.randomUUID());
    }

    @AfterEach
    void bajar() throws BackingStoreException {
        render.bajar();
        aiven.bajar();
        if (encargado != null) encargado.cerrar();
        preferencias.removeNode();
    }

    // Sin el sistema real: un AIVEN_TOKEN en la compu de quien corre los tests no cambia el resultado.
    private Operador abrir(String token) {
        if (token != null) CuentaAiven.guardar(preferencias, "cine-prueba", "mysql-prueba", token);
        Map<String, String> sistema = Map.of();
        return Operador.en(() -> new PanelProduccion(preferencias, sistema::get, render.url(), aiven.url(),
                Duration.ofMillis(50)));
    }

    private static String estadoDeAiven(String estado) {
        return "{\"service\":{\"service_name\":\"mysql-prueba\",\"state\":\"" + estado + "\"}}";
    }

    @Test
    void sinTokenAivenEsDesconocidoYOfreceLaConsola() {
        render.responder("GET /api/generos", 200, "[]");

        encargado = abrir(null);

        encargado.esperarTexto("Desconocido: sin token de Aiven");
        encargado.esperarTexto("En línea");
        assertTrue(encargado.hayBoton("Abrir consola de Aiven"));
        assertEquals(0, aiven.veces("GET " + AIVEN));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "apagada    | POWEROFF   | Apagado",
            "arrancando | REBUILDING | Encendiendo",
            "andando    | RUNNING    | En línea"})
    void elEstadoDeAivenSaleDeSuApi(String caso, String estado, String seVe) {
        render.responder("GET /api/generos", 503, "");
        aiven.responder("GET " + AIVEN, 200, estadoDeAiven(estado));

        encargado = abrir("token-de-prueba");

        encargado.esperarTexto(seVe);
        assertEquals("aivenv1 token-de-prueba", aiven.ultimo("GET " + AIVEN).autorizacion());
        assertFalse(encargado.hayBoton("Abrir consola de Aiven"));
    }

    @Test
    void unTokenInvalidoLoDiceClaro() {
        render.responder("GET /api/generos", 200, "[]");
        aiven.responder("GET " + AIVEN, 401, "{\"message\":\"Invalid token\"}");

        encargado = abrir("vencido");

        encargado.esperarTexto("Error: Aiven rechazó el token: revisá que esté vigente y tenga acceso al proyecto");
        encargado.textos().forEach(t -> assertFalse(t.contains("vencido"), "el token no se muestra: " + t));
    }

    @Test
    void despertarEnciendeLaBaseYDespuesEsperaALaApp() {
        render.responder("GET /api/generos", 503, "");
        aiven.responder("GET " + AIVEN, 200, estadoDeAiven("POWEROFF"));
        aiven.responder("PUT " + AIVEN, 200, estadoDeAiven("REBUILDING"));
        encargado = abrir("token-de-prueba");
        encargado.esperarTexto("Apagado");

        encargado.clic("Despertar");
        encargado.esperar(() -> aiven.veces("PUT " + AIVEN) == 1);
        assertEquals("{\"powered\": true}", aiven.ultimo("PUT " + AIVEN).cuerpo());
        aiven.responder("GET " + AIVEN, 200, estadoDeAiven("RUNNING"));
        encargado.esperar(() -> render.veces("GET /api/generos") >= 2);
        render.responder("GET /api/generos", 200, "[]");

        encargado.esperarTexto("Producción en línea");
        assertTrue(encargado.habilitado("Despertar"));
    }

    @Test
    void sinTokenDespertarSoloEsperaALaApp() {
        render.responder("GET /api/generos", 503, "");
        encargado = abrir(null);
        encargado.esperarTexto("Encendiendo");

        encargado.clic("Despertar");
        render.responder("GET /api/generos", 200, "[]");

        encargado.esperarTexto("Producción en línea");
        assertEquals(0, aiven.veces("PUT " + AIVEN));
    }
}
