package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.EstadoServicio.Fase;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// El estado y el encendido de la base de producción por la API REST de Aiven, que la apaga sin uso en el plan free.
// El token nunca va a un mensaje: solo viaja en el header.
public final class ApiAiven {

    public static final String URL = "https://api.aiven.io";
    public static final String CONSOLA = "https://console.aiven.io";
    private static final Duration ESPERA = Duration.ofSeconds(20);

    private final String recurso;
    private final String token;
    private final String proyecto;
    private final String servicio;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public ApiAiven(String urlBase, String token, String proyecto, String servicio) {
        this.recurso = urlBase.replaceAll("/+$", "") + "/v1/project/" + Parametros.segmento(proyecto)
                + "/service/" + Parametros.segmento(servicio);
        this.token = token;
        this.proyecto = proyecto;
        this.servicio = servicio;
    }

    public EstadoServicio estado() {
        String texto = new String(enviar("GET", HttpRequest.BodyPublishers.noBody()), StandardCharsets.UTF_8);
        String estado;
        try {
            estado = json.readTree(texto).path("service").path("state").asText("");
        } catch (IOException e) {
            throw new ErrorApi(-1, "Aiven devolvió una respuesta que no se pudo leer");
        }
        return switch (estado) {
            case "POWEROFF" -> EstadoServicio.de(Fase.APAGADO);
            case "REBUILDING" -> EstadoServicio.de(Fase.ENCENDIENDO);
            case "RUNNING" -> EstadoServicio.de(Fase.EN_LINEA);
            default -> new EstadoServicio(Fase.DESCONOCIDO, "Aiven dice «" + estado + "»");
        };
    }

    public void encender() {
        enviar("PUT", HttpRequest.BodyPublishers.ofString("{\"powered\": true}", StandardCharsets.UTF_8));
    }

    private byte[] enviar(String metodo, HttpRequest.BodyPublisher cuerpo) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(recurso)).timeout(ESPERA)
                .header("Authorization", "aivenv1 " + token)
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo).build();
        HttpResponse<byte[]> respuesta;
        try {
            respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException e) {
            throw new ErrorApi(0, "Aiven tardó demasiado en responder");
        } catch (IOException e) {
            throw new ErrorApi(0, "No se pudo conectar con Aiven");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorApi(0, "Se canceló el pedido a Aiven");
        }
        int codigo = respuesta.statusCode();
        if (codigo == 401 || codigo == 403) {
            throw new ErrorApi(codigo, "Aiven rechazó el token: revisá que esté vigente y tenga acceso al proyecto");
        }
        if (codigo == 404) {
            throw new ErrorApi(codigo,
                    "Aiven no tiene el servicio «" + servicio + "» en el proyecto «" + proyecto + "»");
        }
        if (codigo >= 400) throw new ErrorApi(codigo, "Aiven contestó " + codigo);
        return respuesta.body();
    }
}
