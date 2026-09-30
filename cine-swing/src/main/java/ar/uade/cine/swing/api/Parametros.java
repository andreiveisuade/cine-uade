package ar.uade.cine.swing.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

// Lo que viaja en la ruta o el cuerpo de un pedido, codificado igual que en api-http.js; sin estado.
final class Parametros {

    private Parametros() {
    }

    /** Arma {@code ?clave=valor&…} salteando los vacíos, igual que {@code consulta()} en api-http.js. */
    static String consulta(Map<String, String> filtros) {
        if (filtros == null) return "";
        String partes = filtros.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue().trim(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return partes.isEmpty() ? "" : "?" + partes;
    }

    // Un código de butaca o de checkout va en la ruta: con URLEncoder un espacio sería "+", que ahí no es espacio.
    static String segmento(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // Map.of no acepta null: un campo vacío viaja como "" y el backend lo rechaza con su propio mensaje.
    static String oVacio(String valor) {
        return valor == null ? "" : valor;
    }
}
