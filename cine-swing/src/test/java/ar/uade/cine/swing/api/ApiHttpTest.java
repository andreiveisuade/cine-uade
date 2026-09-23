package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.Bordero;
import ar.uade.cine.swing.api.dto.Empleado;
import ar.uade.cine.swing.api.dto.Funcion;
import ar.uade.cine.swing.api.dto.PedidoPelicula;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El cliente contra un servidor falso en un puerto libre: se prueba lo que ApiHttp hace con cada respuesta posible
 * sin levantar el backend ni MySQL. Cada test declara qué contesta cada ruta y mira qué pidió el cliente.
 */
class ApiHttpTest {

    private record Recibido(String metodo, String ruta, String autorizacion, String cuerpo) {
    }

    private record Respuesta(int estado, String cuerpo) {
    }

    private static final String BASIC_ENCARGADO = "Basic " + Base64.getEncoder()
            .encodeToString("encargado@cine.uade.ar:cine2026".getBytes(StandardCharsets.UTF_8));

    private HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new LinkedHashMap<>();
    private final List<Recibido> recibidos = new ArrayList<>();
    private ApiHttp api;

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        api = new ApiHttp("http://127.0.0.1:" + servidor.getAddress().getPort() + "/");
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    private void atender(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().toString();
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        synchronized (recibidos) {
            recibidos.add(new Recibido(intercambio.getRequestMethod(), ruta,
                    intercambio.getRequestHeaders().getFirst("Authorization"), cuerpo));
        }
        Respuesta respuesta = respuestas.getOrDefault(intercambio.getRequestMethod() + " " + ruta,
                new Respuesta(404, "{\"error\":\"No existe " + ruta + "\"}"));
        byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
        if (respuesta.estado() == 204) {
            intercambio.sendResponseHeaders(204, -1);
        } else {
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(respuesta.estado(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) intercambio.getResponseBody().write(bytes);
        }
        intercambio.close();
    }

    private void responder(String metodoYRuta, int estado, String cuerpo) {
        respuestas.put(metodoYRuta, new Respuesta(estado, cuerpo));
    }

    private Recibido ultimo() {
        synchronized (recibidos) {
            return recibidos.get(recibidos.size() - 1);
        }
    }

    private void ingresar() {
        responder("POST /api/sesion", 200,
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\"}");
        api.login("encargado@cine.uade.ar", "cine2026");
    }

    @Test
    void elLoginMandaLasCredencialesNuevasEnBasicYEnElCuerpo() {
        responder("POST /api/sesion", 200,
                "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\"}");

        Empleado empleado = api.login("encargado@cine.uade.ar", "cine2026");

        assertEquals("Encargado", empleado.nombre());
        assertTrue(empleado.esAdministrador());
        Recibido login = ultimo();
        assertEquals(BASIC_ENCARGADO, login.autorizacion());
        assertTrue(login.cuerpo().contains("\"email\":\"encargado@cine.uade.ar\""));
        assertTrue(login.cuerpo().contains("\"password\":\"cine2026\""));
        assertTrue(api.tieneCredenciales());
    }

    @Test
    void despuesDelLoginCadaPedidoLlevaBasic() {
        ingresar();
        responder("GET /api/salas", 200, "[]");

        api.obtenerSalas();

        assertEquals(BASIC_ENCARGADO, ultimo().autorizacion());
    }

    @Test
    void unLoginRechazadoNoGuardaCredencialesNiAvisaSesionVencida() {
        responder("POST /api/sesion", 401, "{\"error\":\"Email o contraseña incorrectos\"}");
        AtomicInteger avisos = new AtomicInteger();
        api.alVencerSesion(avisos::incrementAndGet);

        ErrorApi error = assertThrows(ErrorApi.class, () -> api.login("encargado@cine.uade.ar", "mal"));

        assertEquals("Email o contraseña incorrectos", error.getMessage());
        assertEquals(401, error.estado());
        assertFalse(api.tieneCredenciales());
        assertEquals(0, avisos.get());
    }

    @Test
    void unLoginNuevoNoMandaLasCredencialesViejas() {
        ingresar();
        responder("POST /api/sesion", 200,
                "{\"id\":2,\"nombre\":\"Acomodador\",\"email\":\"puerta@cine.uade.ar\",\"rol\":\"ACOMODADOR\"}");

        Empleado otro = api.login("puerta@cine.uade.ar", "otra");

        assertFalse(otro.esAdministrador());
        String esperado = "Basic " + Base64.getEncoder()
                .encodeToString("puerta@cine.uade.ar:otra".getBytes(StandardCharsets.UTF_8));
        assertEquals(esperado, ultimo().autorizacion());
    }

    @Test
    void un401FueraDelLoginOlvidaLasCredencialesYAvisa() {
        ingresar();
        responder("GET /api/salas", 401, "{\"error\":\"Hace falta iniciar sesión para esta operación\"}");
        AtomicInteger avisos = new AtomicInteger();
        api.alVencerSesion(avisos::incrementAndGet);

        ErrorApi error = assertThrows(ErrorApi.class, api::obtenerSalas);

        assertTrue(error.esSesionVencida());
        assertEquals("Hace falta iniciar sesión para esta operación", error.getMessage());
        assertEquals(1, avisos.get());
        assertFalse(api.tieneCredenciales());
    }

    @Test
    void elErrorDelBackendLlegaConSuMensajeIntacto() {
        ingresar();
        responder("DELETE /api/funciones/3", 400,
                "{\"error\":\"La función tiene reservas: no se puede borrar\"}");

        ErrorApi error = assertThrows(ErrorApi.class, () -> api.eliminarFuncion(3));

        assertEquals(400, error.estado());
        assertEquals("La función tiene reservas: no se puede borrar", error.getMessage());
    }

    @Test
    void unErrorSinJsonMuestraElTextoQueVino() {
        ingresar();
        responder("GET /api/salas", 502, "Bad Gateway");

        ErrorApi error = assertThrows(ErrorApi.class, api::obtenerSalas);

        assertEquals(502, error.estado());
        assertEquals("Bad Gateway", error.getMessage());
    }

    @Test
    void unErrorSinCuerpoDiceElCodigo() {
        ingresar();
        responder("GET /api/salas", 500, "");

        ErrorApi error = assertThrows(ErrorApi.class, api::obtenerSalas);

        assertEquals("Error 500 del servidor", error.getMessage());
    }

    @Test
    void unaRespuestaOkQueNoEsJsonSeRechazaConMensajeClaro() {
        ingresar();
        responder("GET /api/salas", 200, "<html>no</html>");

        ErrorApi error = assertThrows(ErrorApi.class, api::obtenerSalas);

        assertEquals("El servidor devolvió una respuesta que no se pudo leer", error.getMessage());
    }

    @Test
    void sinServidorElErrorEsDeConexion() {
        servidor.stop(0);

        ErrorApi error = assertThrows(ErrorApi.class, api::obtenerSalas);

        assertEquals(0, error.estado());
        assertTrue(error.getMessage().startsWith("No se pudo conectar con el servidor"));
    }

    @Test
    void un204OUnNullDevuelvenNull() {
        ingresar();
        responder("DELETE /api/salas/7", 204, "");
        responder("GET /api/funciones/9", 200, "null");

        api.eliminarSala(7);

        assertNull(api.obtenerFuncion(9));
    }

    @Test
    void parseaLaFuncionConSusEmbebidosEIgnoraCamposNuevos() {
        ingresar();
        responder("GET /api/funciones", 200, """
                [{"id":4,"peliculaId":1,"salaId":2,"inicio":"2026-08-13T20:30:00","idioma":"SUBTITULADA",
                  "proyeccion":"TRES_D","precio":5000,"precioDesde":6500,"campoQueTodaviaNoExiste":true,
                  "sala":{"id":2,"nombre":"Sala 2","tipo":"IMAX","butacasPorFila":[10,10],"filas":2,
                          "capacidadSala":20,"minutosLimpieza":15},
                  "pelicula":{"id":1,"titulo":"Matrix","duracionMinutos":136,"generos":["ACCION"],
                              "clasificacion":"MAS_16","enCartelera":true,"estadoRevision":"CONFIRMADA",
                              "puntaje":8.7,"votos":100}}]
                """);

        List<Funcion> funciones = api.obtenerFunciones(null);

        Funcion funcion = funciones.get(0);
        assertEquals(4, funcion.id());
        assertEquals("Matrix", funcion.pelicula().titulo());
        assertEquals(20, funcion.sala().capacidadSala());
        assertEquals(6500, funcion.precioDesde());
        assertNull(funcion.libres());
    }

    @Test
    void parseaElBorderoConSuMapaPorTarifa() {
        ingresar();
        responder("GET /api/funciones/3/bordero", 200, """
                {"funcionId":3,"pelicula":"Matrix","sala":"Sala 1","funcion":"2026-08-13T20:30:00",
                 "generadoEn":"2026-08-13T19:00:00","espectadores":15,"recaudacionBruta":67500,
                 "descuentos":5000,"recaudacionNeta":62500,
                 "porTarifa":{"GENERAL":{"cantidad":12,"total":60000},"JUBILADO":{"cantidad":3,"total":7500}}}
                """);

        Bordero bordero = api.obtenerBordero(3);

        assertEquals(15, bordero.espectadores());
        assertEquals(62500, bordero.recaudacionNeta());
        assertEquals(12, bordero.porTarifa().get("GENERAL").cantidad());
        assertEquals(7500, bordero.porTarifa().get("JUBILADO").total());
    }

    @Test
    void losFiltrosVaciosNoViajanYLosDemasVanCodificados() {
        ingresar();
        responder("GET /api/funciones?salaId=2&desde=2026-08-13", 200, "[]");
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("peliculaId", null);
        filtros.put("salaId", "2");
        filtros.put("desde", " 2026-08-13 ");
        filtros.put("hasta", "");

        api.obtenerFunciones(filtros);

        assertEquals("/api/funciones?salaId=2&desde=2026-08-13", ultimo().ruta());
        assertEquals("?q=la+casa+%26+el+%C3%B1and%C3%BA", ApiHttp.consulta(Map.of("q", "la casa & el ñandú")));
    }

    @Test
    void laEdicionParcialNoMandaLosCamposNulos() {
        ingresar();
        responder("PUT /api/peliculas/5", 200, """
                {"id":5,"titulo":"Matrix","duracionMinutos":136,"generos":["ACCION"],"clasificacion":"MAS_16",
                 "enCartelera":false,"estadoRevision":"CONFIRMADA"}
                """);

        api.actualizarPelicula(5, PedidoPelicula.soloPublicacion(false));

        assertEquals("{\"enCartelera\":false}", ultimo().cuerpo());
    }

    @Test
    void laUrlSeTomaDeLaPropiedadAntesQueDelDefault() {
        String anterior = System.getProperty("cine.api.url");
        try {
            System.setProperty("cine.api.url", "http://otro:9090");
            assertEquals("http://otro:9090", ApiHttp.urlConfigurada());
        } finally {
            if (anterior == null) System.clearProperty("cine.api.url");
            else System.setProperty("cine.api.url", anterior);
        }
    }
}
