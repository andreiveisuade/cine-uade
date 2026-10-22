package ar.uade.cine.infrastructure.pasarelas.mercadopago;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.infrastructure.reloj.Reloj;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.ventas.MedioPago;

// Checkout Pro de Mercado Pago por HTTP (preferencias y pagos); Adapter de PasarelaPagos, sin el SDK de MP.
// El id es el external_reference (va en la back_url); MP no llega a localhost, así que autorizar le pregunta.
@Slf4j
public class MercadoPagoHttp implements PasarelaPagos {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom AZAR = new SecureRandom();
    private static final String MONEDA = "ARS";
    // <reservaId>-<MEDIO>-<azar>: el que arma crear. Cualquier otra referencia es de otra venta de la cuenta.
    private static final String FORMATO_ID = "\\d{1,9}-[A-Z]+-\\d+";
    // Los payment_type_id de MP Argentina: los que no son el medio pedido van en excluded_payment_types.
    private static final List<String> TIPOS_DE_PAGO = List.of("credit_card", "debit_card", "prepaid_card",
            "ticket", "atm", "bank_transfer", "account_money", "digital_currency");
    // MP interpreta la fecha con su zona: sin ella, la toma en la de su servidor y no en la del cine.
    private static final DateTimeFormatter FECHA_MP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // Con sitio local MP rechaza la preferencia si pide auto_return: el comprador vuelve con el botón.
    private static final Set<String> HOSTS_LOCALES = Set.of("localhost", "127.0.0.1");

    private final String token;
    private final String base;
    private final String sitio;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public MercadoPagoHttp(String token, String base, String sitio) {
        this.token = token;
        this.base = sinBarraFinal(base);
        this.sitio = sinBarraFinal(sitio);
    }

    // El monto viaja como un solo ítem: el detalle de butacas ya está en la reserva y el total ya trae el descuento.
    @Override
    public Checkout crear(int reservaId, MedioPago medio, Dinero monto, LocalDateTime vence) {
        String id = reservaId + "-" + medio.name() + "-" + Math.abs(AZAR.nextLong());
        String retorno = sitio + "/api/checkouts/" + id + "/retorno";

        ObjectNode preferencia = JSON.createObjectNode();
        preferencia.putArray("items").addObject()
                .put("title", "Reserva " + reservaId + " · Cine UADE")
                .put("quantity", 1)
                .put("currency_id", MONEDA)
                .put("unit_price", monto.aPesos());
        preferencia.put("external_reference", id);
        preferencia.putObject("back_urls")
                .put("success", retorno)
                .put("failure", retorno)
                .put("pending", retorno);
        ArrayNode excluidos = preferencia.putObject("payment_methods").putArray("excluded_payment_types");
        TIPOS_DE_PAGO.stream()
                .filter(tipo -> !tiposDe(medio).contains(tipo))
                .forEach(tipo -> excluidos.addObject().put("id", tipo));
        preferencia.put("expires", true);
        preferencia.put("expiration_date_to", vence.atZone(Reloj.ZONA).format(FECHA_MP));
        if (!HOSTS_LOCALES.contains(URI.create(sitio).getHost())) {
            preferencia.put("auto_return", "approved");
        }

        JsonNode creada = pedir(HttpRequest.newBuilder(URI.create(base + "/checkout/preferences"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(preferencia.toString())));
        String urlPago = urlDePago(creada);
        return new Checkout(id, reservaId, medio, monto, urlPago, urlPago);
    }

    // El id lo arma crear, pero llega por la URL: solo vale si MP tiene una preferencia con ese external_reference.
    @Override
    public Optional<Checkout> buscar(String checkoutId) {
        if (checkoutId == null || !checkoutId.matches(FORMATO_ID)) {
            return Optional.empty();
        }
        String[] partes = checkoutId.split("-");
        MedioPago medio;
        try {
            medio = MedioPago.valueOf(partes[1]);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        JsonNode resumen = pedir(get("/checkout/preferences/search?external_reference="
                + URLEncoder.encode(checkoutId, StandardCharsets.UTF_8))).path("elements").path(0);
        if (resumen.isMissingNode()) {
            return Optional.empty();
        }
        JsonNode preferencia = pedir(get("/checkout/preferences/" + resumen.path("id").asText()));
        String urlPago = urlDePago(preferencia);
        return Optional.of(new Checkout(checkoutId, Integer.parseInt(partes[0]), medio,
                Dinero.de(preferencia.path("items").path(0).path("unit_price").asDouble()), urlPago, urlPago));
    }

    // Lo que dijo el comprador al volver no cuenta (se escribe a mano): vale lo que MP registró para el
    // external_reference, si coinciden monto, moneda y tipo de pago del medio.
    @Override
    public String autorizar(Checkout checkout) {
        JsonNode pagos = pedir(get("/v1/payments/search?sort=date_created&criteria=desc&external_reference="
                + URLEncoder.encode(checkout.id(), StandardCharsets.UTF_8))).path("results");
        boolean aprobadoQueNoCoincide = false;
        for (JsonNode pago : pagos) {
            if (!"approved".equals(pago.path("status").asText())) {
                continue;
            }
            if (coincide(pago, checkout) && tiposDe(checkout.medio()).contains(pago.path("payment_type_id").asText())) {
                return "MP-" + pago.path("id").asText();
            }
            log.warn("Pago {} de MP aprobado para el checkout {} por {} {} con {}: se esperaban ARS {} con {}",
                    pago.path("id").asText(), checkout.id(), pago.path("currency_id").asText(),
                    pago.path("transaction_amount").asText(), pago.path("payment_type_id").asText(),
                    checkout.monto(), tiposDe(checkout.medio()));
            aprobadoQueNoCoincide = true;
        }
        if (aprobadoQueNoCoincide) {
            throw new ConflictoDeNegocio("Mercado Pago registró un pago que no coincide con la reserva: "
                    + "acercate a la boletería");
        }
        if (pagos.isEmpty()) {
            throw new ConflictoDeNegocio("Mercado Pago todavía no registró el pago: si ya pagaste, "
                    + "recargá la página en un momento");
        }
        throw new ConflictoDeNegocio(switch (pagos.path(0).path("status").asText()) {
            case "rejected", "cancelled" -> "Mercado Pago rechazó el pago: probá de nuevo con otro medio";
            default -> "Mercado Pago todavía no aprobó el pago: recargá la página en unos minutos";
        });
    }

    // account_money va en todos: MP no deja excluirlo y siempre lo ofrece; QR y transferencia también son dinero en cuenta.
    private static Set<String> tiposDe(MedioPago medio) {
        return switch (medio) {
            case DEBITO -> Set.of("debit_card", "account_money");
            case CREDITO -> Set.of("credit_card", "account_money");
            case QR -> Set.of("account_money");
            case TRANSFERENCIA -> Set.of("bank_transfer", "account_money");
            case EFECTIVO -> throw new IllegalStateException("El efectivo no abre checkout");
        };
    }

    // Sin webhook (MP no llega a localhost) la conciliación pregunta. Un checkout con dos aprobados sale una vez.
    @Override
    public List<String> aprobadosDesde(LocalDateTime desde) {
        JsonNode pagos = pedir(get("/v1/payments/search?status=approved&sort=date_created&criteria=desc"
                + "&range=date_created&end_date=NOW&limit=100&begin_date="
                + URLEncoder.encode(desde.atZone(Reloj.ZONA).format(FECHA_MP), StandardCharsets.UTF_8)))
                .path("results");
        List<String> checkouts = new ArrayList<>();
        for (JsonNode pago : pagos) {
            String referencia = pago.path("external_reference").asText();
            if (referencia.matches(FORMATO_ID) && !checkouts.contains(referencia)) {
                checkouts.add(referencia);
            }
        }
        return checkouts;
    }

    private static boolean coincide(JsonNode pago, Checkout checkout) {
        return MONEDA.equals(pago.path("currency_id").asText())
                && Dinero.de(pago.path("transaction_amount").asDouble()).equals(checkout.monto());
    }

    // Con credenciales de prueba (TEST-...) el link que no cobra es el sandbox_init_point.
    private String urlDePago(JsonNode preferencia) {
        return preferencia.path(token.startsWith("TEST-") ? "sandbox_init_point" : "init_point").asText();
    }

    private HttpRequest.Builder get(String ruta) {
        return HttpRequest.newBuilder(URI.create(base + ruta)).GET();
    }

    // Lo que dice el rechazo lo lee el comprador: texto fijo. El detalle de MP va al log.
    private JsonNode pedir(HttpRequest.Builder pedido) {
        HttpRequest armado = pedido
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(armado, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                log.warn("Mercado Pago devolvió {} en {}: {}",
                        respuesta.statusCode(), armado.uri().getPath(), respuesta.body());
                throw new ConflictoDeNegocio("Mercado Pago no respondió bien: probá de nuevo en un rato");
            }
            return JSON.readTree(respuesta.body());
        } catch (IOException e) {
            log.warn("No se pudo llegar a Mercado Pago en {}", armado.uri().getPath(), e);
            throw new ConflictoDeNegocio("No se pudo llegar a Mercado Pago: probá de nuevo en un rato");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictoDeNegocio("Se interrumpió la consulta a Mercado Pago");
        }
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
