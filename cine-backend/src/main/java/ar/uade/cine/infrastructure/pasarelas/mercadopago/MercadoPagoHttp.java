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
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.ventas.MedioPago;

// Checkout Pro de Mercado Pago por HTTP (preferencias y pagos); Adapter de PasarelaPagos, sin el SDK de MP.
// El id del checkout es el external_reference: se conoce antes de crear la preferencia y entra en la back_url.
// MP no llega a localhost con un webhook, así que autorizar no espera aviso: le pregunta a MP por el pago.
@Slf4j
public class MercadoPagoHttp implements PasarelaPagos {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom AZAR = new SecureRandom();

    // Con un sitio local MP rechaza la preferencia entera si pide auto_return: el comprador vuelve con
    // el botón "Volver al sitio".
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
    public Checkout crear(int reservaId, MedioPago medio, Dinero monto) {
        String id = reservaId + "-" + medio.name() + "-" + Math.abs(AZAR.nextLong());
        String retorno = sitio + "/api/checkouts/" + id + "/retorno";

        ObjectNode preferencia = JSON.createObjectNode();
        preferencia.putArray("items").addObject()
                .put("title", "Reserva " + reservaId + " · Cine UADE")
                .put("quantity", 1)
                .put("currency_id", "ARS")
                .put("unit_price", monto.aPesos());
        preferencia.put("external_reference", id);
        preferencia.putObject("back_urls")
                .put("success", retorno)
                .put("failure", retorno)
                .put("pending", retorno);
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
        if (checkoutId == null || !checkoutId.matches("\\d{1,9}-[A-Z]+-\\d+")) {
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

    // Lo que dijo el comprador al volver (la query de la back_url) no cuenta: se puede escribir a mano.
    // Cuenta lo que MP tiene registrado para ese external_reference.
    @Override
    public String autorizar(Checkout checkout) {
        JsonNode pagos = pedir(get("/v1/payments/search?sort=date_created&criteria=desc&external_reference="
                + URLEncoder.encode(checkout.id(), StandardCharsets.UTF_8))).path("results");
        for (JsonNode pago : pagos) {
            if ("approved".equals(pago.path("status").asText())) {
                return "MP-" + pago.path("id").asText();
            }
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
