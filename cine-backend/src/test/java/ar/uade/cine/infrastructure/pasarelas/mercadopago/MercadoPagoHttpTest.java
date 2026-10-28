package ar.uade.cine.infrastructure.pasarelas.mercadopago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 8, 14, 10, 30);

    private HttpServer servidor;
    private final List<String> pedidos = new ArrayList<>();
    private String preferenciaEnviada;
    private String pagosEnMp = "[]";
    private String preferenciasEnMp = "[{\"id\": \"pref-1\"}]";
    private String itemsEnMp = "[{\"unit_price\": 2500.0}]";

    @BeforeEach
    void levantarMercadoPagoFalso() throws IOException {
        // En el wildcard macOS reparte puertos que ya tiene un IPv4 ajeno (Docker) y el pedido le llega a ese.
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
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
                case "/checkout/preferences/pref-1" -> responder(intercambio, 200,
                        "{\"items\": " + itemsEnMp + ", \"init_point\": \"https://mp.test/init\"}");
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
        Checkout checkout = pasarela("http://localhost:8080").crear(25, MedioPago.QR, Dinero.de(7680), Dinero.CERO, VENCE);

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

    // R30: el comprador ve en MP qué es entrada y qué es candy; sin candy, MP no acepta un ítem en cero.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin candy, 0,    1, 7680
            con candy, 5500, 2, 13180
            """)
    void elCandyVaEnSuPropioItem(String caso, double candy, int items, double monto) throws IOException {
        Checkout checkout = pasarela("http://localhost:8080")
                .crear(25, MedioPago.QR, Dinero.de(7680), Dinero.de(candy), VENCE);

        JsonNode enviados = JSON.readTree(preferenciaEnviada).path("items");
        assertEquals(items, enviados.size(), caso);
        assertEquals(7680.0, enviados.path(0).path("unit_price").asDouble(), 0.001, caso);
        if (items == 2) {
            assertEquals(candy, enviados.path(1).path("unit_price").asDouble(), 0.001);
            assertEquals("Candy de la reserva 25", enviados.path(1).path("title").asText());
        }
        assertEquals(Dinero.de(monto), checkout.monto(), caso);
    }

    // MP no debe ofrecer otro medio que el pedido; account_money nunca se excluye (MP responde 400).
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            debito        | DEBITO        | debit_card account_money
            credito       | CREDITO       | credit_card account_money
            qr            | QR            | account_money
            transferencia | TRANSFERENCIA | bank_transfer account_money
            """)
    void laPreferenciaExcluyeLoQueNoEsElMedioPedido(String caso, MedioPago medio, String permitidos)
            throws IOException {
        pasarela("http://localhost:8080").crear(25, medio, Dinero.de(100), Dinero.CERO, VENCE);

        Set<String> excluidos = new HashSet<>();
        JSON.readTree(preferenciaEnviada).at("/payment_methods/excluded_payment_types")
                .forEach(tipo -> excluidos.add(tipo.path("id").asText()));
        Set<String> esperados = new HashSet<>(Set.of("credit_card", "debit_card", "prepaid_card", "ticket",
                "atm", "bank_transfer", "account_money", "digital_currency"));
        esperados.removeAll(Set.of(permitidos.split(" ")));
        assertEquals(esperados, excluidos, caso);
        assertFalse(excluidos.contains("account_money"), caso);
    }

    // R13/R17: pasado el vencimiento la reserva ya soltó las butacas y no hay devolución; MP no debe cobrar.
    @Test
    void laPreferenciaVenceConLaReservaEnHoraArgentina() throws IOException {
        pasarela("http://localhost:8080").crear(25, MedioPago.QR, Dinero.de(100), Dinero.CERO, VENCE);

        JsonNode preferencia = JSON.readTree(preferenciaEnviada);
        assertTrue(preferencia.path("expires").asBoolean());
        assertEquals("2026-08-14T10:30:00.000-03:00", preferencia.path("expiration_date_to").asText());
    }

    // MP rechaza la preferencia entera si pide auto_return con una back_url local.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sitio local,    http://localhost:8080,  false
            loopback,       http://127.0.0.1:8080,  false
            sitio publico,  https://cine.ejemplo.ar, true
            """)
    void elAutoReturnSoloConUnSitioPublico(String caso, String sitio, boolean conAutoReturn) throws IOException {
        pasarela(sitio).crear(25, MedioPago.QR, Dinero.de(100), Dinero.CERO, VENCE);

        assertEquals(conAutoReturn, JSON.readTree(preferenciaEnviada).has("auto_return"), caso);
    }

    @Test
    void conCredencialesDePruebaElLinkEsElDelSandbox() {
        MercadoPagoHttp pasarela = new MercadoPagoHttp("TEST-123", direccion(), "http://localhost:8080");

        assertEquals("https://mp.test/sandbox", pasarela.crear(25, MedioPago.QR, Dinero.de(100), Dinero.CERO, VENCE).urlPago());
    }

    @Test
    void buscarLeeLaReservaYElMedioDelIdYElMontoDeLaPreferencia() {
        Checkout checkout = pasarela("http://localhost:8080").buscar("25-CREDITO-99").orElseThrow();

        assertEquals(25, checkout.reservaId());
        assertEquals(MedioPago.CREDITO, checkout.medio());
        assertEquals(Dinero.de(2500), checkout.monto());
        assertTrue(pedidos.contains("GET /checkout/preferences/search?external_reference=25-CREDITO-99"), pedidos.toString());
    }

    // R30: el candy sumado a una entrada ya paga va solo, y el id dice qué cobrar cuando vuelve.
    @Test
    void elCheckoutSoloDeCandyLlevaUnItemYSeReconoceAlBuscarlo() throws IOException {
        Checkout checkout = pasarela("http://localhost:8080").crearSoloCandy(25, MedioPago.QR, Dinero.de(5500), VENCE);

        JsonNode enviados = JSON.readTree(preferenciaEnviada).path("items");
        assertEquals(1, enviados.size());
        assertEquals("Candy de la reserva 25", enviados.path(0).path("title").asText());
        assertTrue(checkout.id().matches("25-QR-\\d+-CANDY"), checkout.id());
        assertTrue(checkout.soloCandy());
        assertTrue(pasarela("http://localhost:8080").buscar(checkout.id()).orElseThrow().soloCandy());
        assertFalse(pasarela("http://localhost:8080").buscar("25-QR-99").orElseThrow().soloCandy());
    }

    // R30: lo que se valida contra el pago aprobado es todo lo de la preferencia, entradas y candy.
    @Test
    void elMontoDeUnaPreferenciaConCandySumaSusDosItems() {
        itemsEnMp = "[{\"unit_price\": 7680.0}, {\"unit_price\": 5500.0}]";

        assertEquals(Dinero.de(13180), pasarela("http://localhost:8080").buscar("25-QR-99").orElseThrow().monto());
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
                [{"id": 111, "status": "rejected"},
                 {"id": 222, "status": "approved", "transaction_amount": 100.0, "currency_id": "ARS",
                  "payment_type_id": "account_money"}]""";

        assertEquals("MP-222", pasarela("http://localhost:8080").autorizar(checkout()));
        assertTrue(pedidos.stream().anyMatch(p -> p.contains("external_reference=25-QR-7")), pedidos.toString());
    }

    // Lo aprobado debe ser lo pedido: otra preferencia con la misma referencia, u otra moneda, no vale.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            otro monto   | 90.0  | ARS
            otra moneda  | 100.0 | USD
            """)
    void unAprobadoQueNoCoincideConElCheckoutNoAutoriza(String caso, double monto, String moneda) {
        pagosEnMp = """
                [{"id": 222, "status": "approved", "transaction_amount": %s, "currency_id": "%s",
                  "payment_type_id": "account_money"}]"""
                .formatted(monto, moneda);

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> pasarela("http://localhost:8080").autorizar(checkout()), caso);

        assertEquals("Mercado Pago registró un pago que no coincide con la reserva: acercate a la boletería",
                error.getMessage());
    }

    // Un aprobado con otro tipo de pago no paga la promo; account_money cuenta siempre.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            credito en checkout de debito   | DEBITO  | credit_card   | false
            debito en checkout de debito    | DEBITO  | debit_card    | true
            saldo en checkout de debito     | DEBITO  | account_money | true
            debito en checkout de credito   | CREDITO | debit_card    | false
            saldo en checkout de credito    | CREDITO | account_money | true
            tarjeta en checkout de qr       | QR      | credit_card   | false
            """)
    void unAprobadoCuentaSoloConUnTipoDePagoDelMedioDelCheckout(String caso, MedioPago medio, String tipo,
            boolean cuenta) {
        pagosEnMp = """
                [{"id": 222, "status": "approved", "transaction_amount": 100.0, "currency_id": "ARS",
                  "payment_type_id": "%s"}]""".formatted(tipo);
        Checkout checkout = new Checkout("25-" + medio + "-7", 25, medio, Dinero.de(100), "", "", false);

        if (cuenta) {
            assertEquals("MP-222", pasarela("http://localhost:8080").autorizar(checkout), caso);
        } else {
            ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                    () -> pasarela("http://localhost:8080").autorizar(checkout), caso);
            assertEquals("Mercado Pago registró un pago que no coincide con la reserva: acercate a la boletería",
                    error.getMessage());
        }
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

    // La conciliación: los pagos aprobados desde un momento, con la referencia de un checkout nuestro.
    @Test
    void losAprobadosDesdeUnMomentoSonLosCheckoutsConPagoAprobado() {
        pagosEnMp = """
                [{"id": 1, "status": "approved", "external_reference": "25-QR-7"},
                 {"id": 2, "status": "approved", "external_reference": "venta-de-otro-sistema"},
                 {"id": 3, "status": "approved", "external_reference": "25-QR-7"},
                 {"id": 4, "status": "approved", "external_reference": "26-DEBITO-8"}]""";

        List<String> aprobados = pasarela("http://localhost:8080").aprobadosDesde(VENCE);

        assertEquals(List.of("25-QR-7", "26-DEBITO-8"), aprobados);
        String pedido = URLDecoder.decode(pedidos.get(pedidos.size() - 1), StandardCharsets.UTF_8);
        assertTrue(pedido.contains("status=approved"), pedido);
        assertTrue(pedido.contains("begin_date=2026-08-14T10:30:00.000-03:00"), pedido);
    }

    @Test
    void unErrorDeMpNoLeMuestraAlCompradorLaRespuestaCruda() {
        servidor.removeContext("/");
        servidor.createContext("/", intercambio -> responder(intercambio, 401, "{\"message\": \"invalid token\"}"));

        ConflictoDeNegocio error = assertThrows(ConflictoDeNegocio.class,
                () -> pasarela("http://localhost:8080").crear(25, MedioPago.QR, Dinero.de(100), Dinero.CERO, VENCE));

        assertEquals("Mercado Pago no respondió bien: probá de nuevo en un rato", error.getMessage());
        assertFalse(error.getMessage().contains("token"));
    }

    private static Checkout checkout() {
        return new Checkout("25-QR-7", 25, MedioPago.QR, Dinero.de(100), "", "", false);
    }

    private MercadoPagoHttp pasarela(String sitio) {
        return new MercadoPagoHttp("APP_USR-123", direccion(), sitio);
    }

    private String direccion() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
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
