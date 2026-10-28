package ar.uade.cine.swing.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

// Lo que ve el encargado ante una respuesta de error: el {error} del backend o un texto por código.
// Nunca el cuerpo crudo: el HTML de nginx va a la consola.
final class MensajeDeError {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MensajeDeError() {
    }

    static String de(int estado, byte[] cuerpo) {
        String texto = new String(cuerpo, StandardCharsets.UTF_8).strip();
        try {
            JsonNode datos = JSON.readTree(texto);
            if (datos != null && datos.hasNonNull("error")) return datos.get("error").asText();
        } catch (JsonProcessingException e) {
            // No es JSON: se resuelve abajo, por el código.
        }
        if (!texto.isEmpty()) System.err.println("Respuesta " + estado + " sin mensaje de error en JSON: " + texto);
        return switch (estado) {
            case 502, 503, 504 -> "El servidor no está disponible en este momento: probá de nuevo en unos segundos";
            case 500 -> "Falló el servidor: probá de nuevo y, si sigue pasando, avisá al administrador";
            case 404 -> "No se encontró el recurso en el servidor";
            default -> "El servidor respondió con un error (código " + estado + ")";
        };
    }
}
