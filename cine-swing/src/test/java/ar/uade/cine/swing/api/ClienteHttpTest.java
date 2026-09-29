package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.ServidorFalso.Recibido;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que es del transporte y no de una ruta: las credenciales, el 401, los errores con y sin JSON y la conexión. Las
 * operaciones que se usan son cualquiera; lo que se mira es lo que hace ClienteHttp con cada respuesta.
 */
class ClienteHttpTest {

    private static final String BASIC_ENCARGADO = "Basic " + Base64.getEncoder()
            .encodeToString("encargado@cine.uade.ar:cine2026".getBytes(StandardCharsets.UTF_8));

    private ServidorFalso servidor;
    private ClienteHttp http;
    private ApiSesion sesion;
    private ApiSalas salas;
    private ApiFunciones funciones;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        http = new ClienteHttp(servidor.url());
        sesion = new ApiSesion(http);
        salas = new ApiSalas(http);
        funciones = new ApiFunciones(http);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    // Lo que ClienteHttp guardó se ve en el pedido siguiente: el header que lleva, o ninguno.
    private String autorizacionDelSiguientePedido() {
        servidor.responder("GET /api/generos", 200, "[]");
        new ApiCatalogos(http).obtenerGeneros();
        return servidor.ultimo().autorizacion();
    }

    private void ingresar() {
        servidor.responder("POST /api/sesion", 200,
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\"}");
        sesion.login("encargado@cine.uade.ar", "cine2026");
    }

    @Test
    void elLoginMandaLasCredencialesNuevasSoloEnBasic() {
        servidor.responder("POST /api/sesion", 200,
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\"}");

        Empleado empleado = sesion.login("encargado@cine.uade.ar", "cine2026");

        assertEquals("Encargado", empleado.nombre());
        assertTrue(empleado.esAdministrador());
        Recibido login = servidor.ultimo();
        assertEquals(BASIC_ENCARGADO, login.autorizacion());
        assertTrue(login.cuerpo().isEmpty(), "la contraseña no viaja en el cuerpo");
        assertEquals(BASIC_ENCARGADO, autorizacionDelSiguientePedido());
    }

    @Test
    void despuesDelLoginCadaPedidoLlevaBasic() {
        ingresar();
        servidor.responder("GET /api/salas", 200, "[]");

        salas.obtenerSalas();

        assertEquals(BASIC_ENCARGADO, servidor.ultimo().autorizacion());
    }

    @Test
    void unLoginRechazadoNoGuardaCredencialesNiAvisaSesionVencida() {
        servidor.responder("POST /api/sesion", 401, "{\"error\":\"Email o contraseña incorrectos\"}");
        AtomicInteger avisos = new AtomicInteger();
        http.alVencerSesion(avisos::incrementAndGet);

        ErrorApi error = assertThrows(ErrorApi.class, () -> sesion.login("encargado@cine.uade.ar", "mal"));

        assertEquals("Email o contraseña incorrectos", error.getMessage());
        assertEquals(401, error.estado());
        assertEquals(0, avisos.get());
        assertNull(autorizacionDelSiguientePedido());
    }

    @Test
    void unLoginNuevoNoMandaLasCredencialesViejas() {
        ingresar();
        servidor.responder("POST /api/sesion", 200,
                "{\"id\":2,\"nombre\":\"Acomodador\",\"email\":\"puerta@cine.uade.ar\",\"rol\":\"ACOMODADOR\"}");

        Empleado otro = sesion.login("puerta@cine.uade.ar", "otra");

        assertFalse(otro.esAdministrador());
        String esperado = "Basic " + Base64.getEncoder()
                .encodeToString("puerta@cine.uade.ar:otra".getBytes(StandardCharsets.UTF_8));
        assertEquals(esperado, servidor.ultimo().autorizacion());
    }

    @Test
    void un401FueraDelLoginOlvidaLasCredencialesYAvisa() {
        ingresar();
        servidor.responder("GET /api/salas", 401, "{\"error\":\"Hace falta iniciar sesión para esta operación\"}");
        AtomicInteger avisos = new AtomicInteger();
        http.alVencerSesion(avisos::incrementAndGet);

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertTrue(error.esSesionVencida());
        assertEquals("Hace falta iniciar sesión para esta operación", error.getMessage());
        assertEquals(1, avisos.get());
        assertNull(autorizacionDelSiguientePedido());
    }

    @Test
    void elErrorDelBackendLlegaConSuMensajeIntacto() {
        ingresar();
        servidor.responder("DELETE /api/funciones/3", 400,
                "{\"error\":\"La función tiene reservas: no se puede borrar\"}");

        ErrorApi error = assertThrows(ErrorApi.class, () -> funciones.eliminarFuncion(3));

        assertEquals(400, error.estado());
        assertEquals("La función tiene reservas: no se puede borrar", error.getMessage());
    }

    @Test
    void elHtmlDeUnProxyCaidoNoLlegaALaPantalla() {
        ingresar();
        servidor.responder("GET /api/salas", 502, "<html><head><title>502 Bad Gateway</title></head><body>"
                + "<center><h1>502 Bad Gateway</h1></center><hr><center>nginx</center></body></html>");

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals(502, error.estado());
        assertEquals("El servidor no está disponible en este momento. Probá de nuevo en unos segundos.",
                error.getMessage());
    }

    @Test
    void unErrorSinCuerpoSeExplicaPorElCodigo() {
        ingresar();
        servidor.responder("GET /api/salas", 500, "");

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals("Falló el servidor. Probá de nuevo; si sigue pasando, avisá al administrador.",
                error.getMessage());
    }

    @Test
    void un404ConHtmlEsUnRecursoQueNoEsta() {
        ingresar();
        servidor.responder("GET /api/salas", 404, "<html><body>Not Found</body></html>");

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals("No se encontró el recurso en el servidor.", error.getMessage());
    }

    @Test
    void unCodigoSinMensajeConocidoLoNombra() {
        ingresar();
        servidor.responder("GET /api/salas", 418, "tetera");

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals("El servidor respondió con un error (código 418).", error.getMessage());
    }

    @Test
    void unaRespuestaOkQueNoEsJsonSeRechazaConMensajeClaro() {
        ingresar();
        servidor.responder("GET /api/salas", 200, "<html>no</html>");

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals("El servidor devolvió una respuesta que no se pudo leer", error.getMessage());
    }

    @Test
    void sinServidorElErrorEsDeConexion() {
        servidor.bajar();

        ErrorApi error = assertThrows(ErrorApi.class, salas::obtenerSalas);

        assertEquals(0, error.estado());
        assertTrue(error.getMessage().startsWith("No se pudo conectar con el servidor en http://127.0.0.1:"));
        assertTrue(error.getMessage().endsWith("¿Está levantado?"));
    }

    @Test
    void un204OUnNullDevuelvenNull() {
        ingresar();
        servidor.responder("DELETE /api/salas/7", 204, "");
        servidor.responder("GET /api/funciones/9", 200, "null");

        salas.eliminarSala(7);

        assertNull(funciones.obtenerFuncion(9));
    }

    @Test
    void laUrlSeTomaDeLaPropiedadAntesQueDelDefault() {
        String anterior = System.getProperty("cine.api.url");
        try {
            System.setProperty("cine.api.url", "http://otro:9090");
            assertEquals("http://otro:9090", ClienteHttp.urlConfigurada());
        } finally {
            if (anterior == null) System.clearProperty("cine.api.url");
            else System.setProperty("cine.api.url", anterior);
        }
    }
}
