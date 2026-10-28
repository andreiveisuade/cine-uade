package ar.uade.cine.swing.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
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
import java.util.UUID;

// El transporte con el backend: Basic, JSON y el 401; lo comparten todas las Api, que solo saben de rutas.
// Bloqueante a propósito: quien llama lo corre fuera del EDT (comun.Tarea).
public final class ClienteHttp {

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

    /** Credenciales nuevas en Basic, nunca las viejas; se guardan solo si el login contestó bien (un 401 sale antes). */
    <T> T ingresar(String ruta, String email, String password, Class<T> tipo) {
        return ingresar(ruta, email, password, tipo, ESPERA);
    }

    <T> T ingresar(String ruta, String email, String password, Class<T> tipo, Duration espera) {
        String nuevas = Base64.getEncoder().encodeToString(
                (Parametros.oVacio(email) + ":" + Parametros.oVacio(password)).getBytes(StandardCharsets.UTF_8));
        T respuesta = pedir("POST", ruta, null, tipo(tipo), nuevas, false, espera);
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

    /** Un archivo en multipart/form-data, sin librerías: java.net.http no lo arma solo. */
    <T> T subir(String ruta, String campo, String nombreArchivo, String tipoArchivo, byte[] datos, Class<T> tipo) {
        String limite = UUID.randomUUID().toString();
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        cuerpo.writeBytes(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + campo + "\"; filename=\""
                + nombreArchivo + "\"\r\nContent-Type: " + tipoArchivo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.writeBytes(datos);
        cuerpo.writeBytes(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return leer(enviar("POST", ruta, "multipart/form-data; boundary=" + limite,
                HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()), credenciales, true, ESPERA), tipo(tipo));
    }

    /** El cuerpo crudo, para lo que no es JSON (una imagen). */
    byte[] bytes(String ruta) {
        return enviar("GET", ruta, null, HttpRequest.BodyPublishers.noBody(), credenciales, true, ESPERA).body();
    }

    /** Una imagen de otro sitio, como un póster de TMDB: sin credenciales, que son solo para el backend. */
    byte[] bytesDeAfuera(String url) {
        try {
            HttpResponse<byte[]> respuesta = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(ESPERA)
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.statusCode() >= 400) throw new ErrorApi(respuesta.statusCode(), "No se pudo bajar la imagen");
            return respuesta.body();
        } catch (IOException | IllegalArgumentException e) {
            throw new ErrorApi(0, "No se pudo bajar la imagen");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorApi(0, "Se canceló la descarga de la imagen");
        }
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
        return leer(cuerpo == null
                ? enviar(metodo, ruta, null, HttpRequest.BodyPublishers.noBody(), clave, avisaVencida, espera)
                : enviar(metodo, ruta, "application/json",
                        HttpRequest.BodyPublishers.ofString(escribir(cuerpo), StandardCharsets.UTF_8),
                        clave, avisaVencida, espera), tipo);
    }

    private <T> T leer(HttpResponse<byte[]> respuesta, JavaType tipo) {
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
    private HttpResponse<byte[]> enviar(String metodo, String ruta, String tipoCuerpo,
                                        HttpRequest.BodyPublisher cuerpo, String clave,
                                        boolean avisaVencida, Duration espera) {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + ruta)).timeout(espera);
        if (clave != null) pedido.header("Authorization", "Basic " + clave);
        if (tipoCuerpo != null) pedido.header("Content-Type", tipoCuerpo);
        pedido.method(metodo, cuerpo);

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
        if (estado >= 400) throw new ErrorApi(estado, MensajeDeError.de(estado, respuesta.body()));
        return respuesta;
    }

    private String escribir(Object cuerpo) {
        try {
            return json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el JSON del pedido", e);
        }
    }
}
