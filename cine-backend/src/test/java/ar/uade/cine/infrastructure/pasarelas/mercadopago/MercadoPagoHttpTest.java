package ar.uade.cine.infrastructure.pasarelas.mercadopago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos.Checkout;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.ventas.MedioPago;

class MercadoPagoHttpTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer servidor;
    private final List<String> pedidos = new ArrayList<>();
    private String preferenciaEnviada;
    private String pagosEnMp = "[]";
    private String preferenciasEnMp = "[{\"id\": \"pref-1\"}]";

    @BeforeEach
    void levantarMercadoPagoFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(0), 0);
        servidor.createContext("/", intercambio -> {
            pedidos.add(intercambio.getRequestMethod() + " " + intercambio.getRequestURI());
            switch (intercambio.getRequestURI().getPath()) {
                case "/checkout/preferences" -> {
                    preferenciaEnviada = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    responder(intercambio, 201, """
                            {"id": "pref-1", "init_point": "https://mp.test/init",
                             "sandbox_init_point": "https://mp.test/sandbox"}""");
                }
                case "/checkout/preferences/search" ->
                        responder(intercambio, 200, "{\"elements\": " + preferenciasEnMp + "}");
                case "/checkout/preferences/pref-1" -> responder(intercambio, 200, """
                        {"items": [{"unit_price": 2500.0}], "init_point": "https://mp.test/init"}""");
                case "/v1/payments/search" -> responder(intercambio, 200, "{\"results\": " + pagosEnMp + "}");
                default -> responder(intercambio, 404, "{}");
            }
        });
        servidor.start();
    }

    @AfterEach
    void bajarElServidor() {
        servidor.stop(0);
    }

    @Test
    void laPreferenciaLlevaElMontoLaReferenciaYLaVueltaAlBackend() throws IOException {
        Checkout checkout = pasarela("http://localhost:8080").crear(25, MedioPago.QR, Dinero.de(7680));

        JsonNode preferencia = JSON.readTree(preferenciaEnviada);
        assertEquals(7680.0, preferencia.at("/items/0/unit_price").asDouble(), 0.001);
        assertEquals("ARS", preferencia.at("/items/0/currency_id").asText());
        assertEquals(checkout.id(), preferencia.get("external_reference").asText());
        assertTrue(checkout.id().startsWith("25-QR-"), checkout.id());
        String retorno = "http://localhost:8080/api/checkouts/" + checkout.id() + "/retorno";
        for (String caso : List.of("success", "failure", "pending")) {
            assertEquals(retorno, preferencia.at("/back_urls/" + caso).asText(), caso);
        }
        assertEquals("https://mp.test/init", checkout.urlPago());
        assertEquals(Dinero.de(7680), checkout.monto());
    }

    // MP rechaza la preferencia entera si pide auto_return con una back_url local.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sitio local,    http://localhost:8080,  false
            loopback,       http://127.0.0.1:8080,  false
            sitio publico,  https://cine.ejemplo.ar, true
            """)
    void elAutoReturnSoloConUnSitioPublico(String caso, String sitio, boolean conAutoReturn) throws IOException {
        pasarela(sitio).crear(25, MedioPago.QR, Dinero.de(100));

        assertEquals(conAutoReturn, JSON.readTree(preferenciaEnviada).has("auto_return"), caso);
    }

    @Test
    void conCredencialesDePruebaElLinkEsElDelSandbox() {
        MercadoPagoHttp pasarela = new MercadoPagoHttp("TEST-123", direccion(), "http://localhost:8080");

        assertEquals("https://mp.test/sandbox", pasarela.crear(25, MedioPago.QR, Dinero.de(100)).urlPago());
    }

    @Test
    void buscarLeeLaReservaYElMedioDelIdYElMontoDeLaPreferencia() {
        Checkout checkout = pasarela("http://localhost:8080").buscar("25-CREDITO-99").orElseThrow();

        assertEquals(25, checkout.reservaId());
        assertEquals(MedioPago.CREDITO, checkout.medio());
        assertEquals(Dinero.de(2500), checkout.monto());
        assertTrue(pedidos.contains("GET /checkout/preferences/search?external_reference=25-CREDITO-99"), pedidos.toString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            formato ajeno,              pref-1
            medio que no existe,        25-CRIPTO-99
            sin preferencia en MP,      25-QR-99
            """)
    void unCheckoutQueMpNoConoceNoExiste(String caso, String id) {
        preferenciasEnMp = "[]";

        assertTrue(pasarela("http://localhost:8080").buscar(id).isEmpty(), caso);
    }

    @Test
    void unPagoAprobadoDaElCodigoDeAutorizacion() {
        pagosEnMp = """
                [{"id": 111, "status": "rejected"}, {"id": 222, "status": "approved"}]""";

        assertEquals("MP-222", pasarela("http://localhost:8080").autorizar(checkout()));
        assertTrue(pedidos.stream().anyMatch(p -> p.contains("external_reference=25-QR-7")), pedidos.toString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            rechazado    | [{"id": 1, "status": "rejected"}]   | Mercado Pago rechazó el pago: probá de nuevo con otro medio
            pendiente    | [{"id": 1, "status": "pending"}]    | Mercado Pago todavía no aprobó el pago: recargá la página en unos minutos
            en proceso   | [{"id": 1, "status": "in_process"}] | Mercado Pago todavía no aprobó el pago: recargá la página en unos minutos
            sin pagos    | []                                  | Mercado Pago todavía no registró el pago: si ya pagaste, recargá la página en un momento
            """)
    void sinPagoAprobadoNoHayAutorizacion(String caso, String pagos, String mensaje) {
        pagosEnMp = pagos;

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> pasarela("http://localhost:8080").autorizar(checkout()), caso);

        assertEquals(mensaje, error.getMessage());
    }

    @Test
    void unErrorDeMpNoLeMuestraAlCompradorLaRespuestaCruda() {
        servidor.removeContext("/");
        servidor.createContext("/", intercambio -> responder(intercambio, 401, "{\"message\": \"invalid token\"}"));

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> pasarela("http://localhost:8080").crear(25, MedioPago.QR, Dinero.de(100)));

        assertEquals("Mercado Pago no respondió bien: probá de nuevo en un rato", error.getMessage());
        assertFalse(error.getMessage().contains("token"));
    }

    private static Checkout checkout() {
        return new Checkout("25-QR-7", 25, MedioPago.QR, Dinero.de(100), "", "");
    }

    private MercadoPagoHttp pasarela(String sitio) {
        return new MercadoPagoHttp("APP_USR-123", direccion(), sitio);
    }

    private String direccion() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    private static void responder(HttpExchange intercambio, int codigo, String cuerpo) {
        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
        try (OutputStream salida = intercambio.getResponseBody()) {
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(codigo, datos.length);
            salida.write(datos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
