package ar.uade.cine.swing.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** El backend de mentira en un puerto libre: cada test declara qué contesta cada ruta y mira qué le pidieron. */
public final class ServidorFalso {

    public record Recibido(String metodo, String ruta, String autorizacion, String tipoCuerpo, String cuerpo) {
    }

    private record Respuesta(int estado, String cuerpo) {
    }

    private final HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new LinkedHashMap<>();
    private final List<Recibido> recibidos = new ArrayList<>();

    public ServidorFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
    }

    public void bajar() {
        servidor.stop(0);
    }

    public void responder(String metodoYRuta, int estado, String cuerpo) {
        respuestas.put(metodoYRuta, new Respuesta(estado, cuerpo));
    }

    public Recibido ultimo() {
        synchronized (recibidos) {
            return recibidos.get(recibidos.size() - 1);
        }
    }

    private void atender(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().toString();
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        synchronized (recibidos) {
            recibidos.add(new Recibido(intercambio.getRequestMethod(), ruta,
                    intercambio.getRequestHeaders().getFirst("Authorization"),
                    intercambio.getRequestHeaders().getFirst("Content-Type"), cuerpo));
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
}
