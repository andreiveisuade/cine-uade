package ar.uade.cine.swing.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

// El transporte con el backend: Basic, JSON y errores; lo comparten todas las Api, que solo saben de rutas.
/**
 * La mitad de {@code cine-frontend/src/api/api-http.js} que no depende de ninguna ruta: {@code pedir()} y
 * {@code mensajeDeError()}. Las operaciones viven en una {@code Api} por subdominio, y cada pantalla recibe solo las
 * que usa. Es bloqueante a propósito: quien llama la corre fuera del EDT (ver {@code comun.Tarea}), y así los tests la
 * usan sin ventanas.
 */
public final class ClienteHttp {

    public static final String URL_POR_DEFECTO = "http://localhost:8080";
    private static final Duration ESPERA = Duration.ofSeconds(30);

    private final String base;
    private final HttpClient http;
    private final ObjectMapper json;
    // El backend no guarda sesión: cada pedido lleva Basic. Vive en memoria y se va al cerrar la app.
    private volatile String credenciales;
    private volatile Runnable alVencerSesion = () -> { };

    public ClienteHttp(String urlBase) {
        this.base = urlBase.replaceAll("/+$", "") + "/api";
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        // El backend suma campos sin avisar (asientos, puntaje): ignorarlos evita que un cambio allá rompa acá.
        this.json = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                // Un null no viaja: en el PUT parcial de película significa "no tocar".
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** Propiedad {@code -Dcine.api.url}, si no la variable {@code CINE_API_URL}, si no el backend local del docker. */
    public static String urlConfigurada() {
        String propiedad = System.getProperty("cine.api.url");
        if (propiedad != null && !propiedad.isBlank()) return propiedad.trim();
        String entorno = System.getenv("CINE_API_URL");
        if (entorno != null && !entorno.isBlank()) return entorno.trim();
        return URL_POR_DEFECTO;
    }

    public String urlBase() {
        return base;
    }

    /** Se llama ante un 401 fuera del login, desde el hilo del pedido: quien lo registra salta al EDT. */
    public void alVencerSesion(Runnable accion) {
        this.alVencerSesion = accion;
    }

    public void olvidarCredenciales() {
        credenciales = null;
    }

    /**
     * Las credenciales nuevas van en Basic, que es lo único que valida el backend (Spring Security). Nunca las viejas:
     * si vencieron, el filtro rechazaría el pedido antes de probar las nuevas. Se guardan solo si el login contestó
     * bien: un 401 sale por excepción antes de llegar a guardarlas, y no avisa sesión vencida porque acá quiere decir
     * "clave incorrecta".
     */
    <T> T ingresar(String ruta, String email, String password, Class<T> tipo) {
        String nuevas = Base64.getEncoder().encodeToString(
                (Parametros.oVacio(email) + ":" + Parametros.oVacio(password)).getBytes(StandardCharsets.UTF_8));
        T respuesta = pedir("POST", ruta, null, tipo(tipo), nuevas, false, ESPERA);
        credenciales = nuevas;
        return respuesta;
    }

    <T> T get(String ruta, Class<T> tipo) {
        return pedir("GET", ruta, null, tipo(tipo));
    }

    <T> List<T> lista(String ruta, Class<T> elemento) {
        return pedir("GET", ruta, null, json.getTypeFactory().constructCollectionType(List.class, elemento));
    }

    <T> T post(String ruta, Object cuerpo, Class<T> tipo) {
        return pedir("POST", ruta, cuerpo, tipo(tipo));
    }

    /** Para lo que contesta recién al terminar un trabajo largo, que la espera normal cortaría. */
    <T> T post(String ruta, Object cuerpo, Class<T> tipo, Duration espera) {
        return pedir("POST", ruta, cuerpo, tipo(tipo), credenciales, true, espera);
    }

    <T> T put(String ruta, Object cuerpo, Class<T> tipo) {
        return pedir("PUT", ruta, cuerpo, tipo(tipo));
    }

    <T> T patch(String ruta, Object cuerpo, Class<T> tipo) {
        return pedir("PATCH", ruta, cuerpo, tipo(tipo));
    }

    void delete(String ruta) {
        pedir("DELETE", ruta, null, tipo(JsonNode.class));
    }

    private JavaType tipo(Class<?> clase) {
        return json.getTypeFactory().constructType(clase);
    }

    private <T> T pedir(String metodo, String ruta, Object cuerpo, JavaType tipo) {
        return pedir(metodo, ruta, cuerpo, tipo, credenciales, true, ESPERA);
    }

    // `avisaVencida`: un 401 del login es "clave incorrecta", no una sesión que se cayó.
    private <T> T pedir(String metodo, String ruta, Object cuerpo, JavaType tipo, String clave,
                        boolean avisaVencida, Duration espera) {
        HttpResponse<byte[]> respuesta = enviar(metodo, ruta, cuerpo, clave, avisaVencida, espera);
        String texto = new String(respuesta.body(), StandardCharsets.UTF_8);
        if (texto.isBlank()) return null;
        try {
            JsonNode datos = json.readTree(texto);
            return datos.isNull() ? null : json.readerFor(tipo).readValue(datos);
        } catch (IOException e) {
            throw new ErrorApi(respuesta.statusCode(), "El servidor devolvió una respuesta que no se pudo leer");
        }
    }

    /** Manda el pedido y convierte cualquier estado de error en {@link ErrorApi}; el cuerpo de un 2xx queda crudo. */
    private HttpResponse<byte[]> enviar(String metodo, String ruta, Object cuerpo, String clave,
                                        boolean avisaVencida, Duration espera) {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + ruta)).timeout(espera);
        if (clave != null) pedido.header("Authorization", "Basic " + clave);
        if (cuerpo != null) {
            pedido.header("Content-Type", "application/json");
            pedido.method(metodo, HttpRequest.BodyPublishers.ofString(escribir(cuerpo), StandardCharsets.UTF_8));
        } else {
            pedido.method(metodo, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<byte[]> respuesta;
        try {
            respuesta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException e) {
            throw new ErrorApi(0, "El servidor tardó demasiado en responder");
        } catch (IOException e) {
            throw new ErrorApi(0, "No se pudo conectar con el servidor en " + base + ". ¿Está levantado?");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorApi(0, "Se canceló el pedido al servidor");
        }

        int estado = respuesta.statusCode();
        // 401 fuera del login: las credenciales dejaron de valer. Se olvidan y la app vuelve al login.
        if (estado == 401 && avisaVencida) {
            credenciales = null;
            alVencerSesion.run();
        }
        if (estado >= 400) throw new ErrorApi(estado, mensajeDeError(estado, respuesta.body()));
        return respuesta;
    }

    /**
     * El {@code {"error": "..."}} del backend va tal cual. Cualquier otra cosa (el HTML de un nginx con el backend
     * reiniciando, texto, cuerpo vacío) no se muestra: va a la consola para depurar y en pantalla queda un mensaje
     * según el código.
     */
    private String mensajeDeError(int estado, byte[] cuerpo) {
        String texto = new String(cuerpo, StandardCharsets.UTF_8).strip();
        try {
            JsonNode datos = json.readTree(texto);
            if (datos != null && datos.hasNonNull("error")) return datos.get("error").asText();
        } catch (JsonProcessingException e) {
            // No es JSON: se resuelve abajo, por el código.
        }
        if (!texto.isEmpty()) System.err.println("Respuesta " + estado + " sin mensaje de error en JSON: " + texto);
        return switch (estado) {
            case 502, 503, 504 -> "El servidor no está disponible en este momento. Probá de nuevo en unos segundos.";
            case 500 -> "Falló el servidor. Probá de nuevo; si sigue pasando, avisá al administrador.";
            case 404 -> "No se encontró el recurso en el servidor.";
            default -> "El servidor respondió con un error (código " + estado + ").";
        };
    }

    private String escribir(Object cuerpo) {
        try {
            return json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el JSON del pedido", e);
        }
    }
}
