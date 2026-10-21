package ar.uade.cine.swing.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

// Una fila de prueba leída como la lee ClienteHttp: el JSON dice solo los campos que le importan al test.
public final class Json {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }

    public static <T> T leer(String json, Class<T> tipo) {
        try {
            return JSON.readValue(json, tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException(json, e);
        }
    }
}
