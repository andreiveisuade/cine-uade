package ar.uade.cine.swing.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

// Si la app de producción contesta; Render Free la apaga sin tráfico y un pedido cualquiera la despierta.
public final class ApiRender {

    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ApiRender(String urlBase) {
        this.base = urlBase.replaceAll("/+$", "");
    }

    /** Una ruta pública y liviana: 200 es en línea; si no contesta a tiempo, el mismo pedido la está despertando. */
    public EstadoServicio estado(Duration espera) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(base + "/api/generos")).timeout(espera).GET().build();
        int codigo;
        try {
            codigo = http.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (HttpTimeoutException e) {
            return EstadoServicio.de(EstadoServicio.Fase.ENCENDIENDO);
        } catch (IOException e) {
            throw new ErrorApi(0, "No se pudo conectar con Render en " + base);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorApi(0, "Se canceló el pedido a Render");
        }
        if (codigo == 200) return EstadoServicio.de(EstadoServicio.Fase.EN_LINEA);
        // El proxy de Render contesta así mientras la instancia arranca.
        if (codigo == 502 || codigo == 503 || codigo == 504) return EstadoServicio.de(EstadoServicio.Fase.ENCENDIENDO);
        throw new ErrorApi(codigo, "Render contestó " + codigo + " en /api/generos");
    }
}
