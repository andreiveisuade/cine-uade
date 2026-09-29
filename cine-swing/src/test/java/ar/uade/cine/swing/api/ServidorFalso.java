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

/**
 * El backend de mentira en un puerto libre: cada test declara qué contesta cada ruta y después mira qué le pidieron.
 * Así se prueba el cliente con cada respuesta posible sin levantar el backend ni MySQL.
 */
final class ServidorFalso {

    record Recibido(String metodo, String ruta, String autorizacion, String cuerpo) {
    }

    private record Respuesta(int estado, String cuerpo) {
    }

    private final HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new LinkedHashMap<>();
    private final List<Recibido> recibidos = new ArrayList<>();

    ServidorFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/";
    }

    void bajar() {
        servidor.stop(0);
    }

    void responder(String metodoYRuta, int estado, String cuerpo) {
        respuestas.put(metodoYRuta, new Respuesta(estado, cuerpo));
    }

    Recibido ultimo() {
        synchronized (recibidos) {
            return recibidos.get(recibidos.size() - 1);
        }
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
}
