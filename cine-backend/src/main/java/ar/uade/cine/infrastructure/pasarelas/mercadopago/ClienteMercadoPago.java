package ar.uade.cine.infrastructure.pasarelas.mercadopago;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.uade.cine.model.rechazos.ConflictoDeNegocio;

// El transporte con la API de Mercado Pago: Bearer, timeouts, JSON y el rechazo si no contesta bien.
@Slf4j
final class ClienteMercadoPago {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String token;
    private final String base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    ClienteMercadoPago(String token, String base) {
        this.token = token;
        this.base = base;
    }

    boolean conCredencialesDePrueba() {
        return token.startsWith("TEST-");
    }

    JsonNode get(String ruta) {
        return pedir(HttpRequest.newBuilder(URI.create(base + ruta)).GET());
    }

    JsonNode post(String ruta, String cuerpo) {
        return pedir(HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)));
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
}
