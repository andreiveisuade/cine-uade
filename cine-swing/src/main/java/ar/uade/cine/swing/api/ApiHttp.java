package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.Arqueo;
import ar.uade.cine.swing.api.dto.ArqueoCandy;
import ar.uade.cine.swing.api.dto.Bordero;
import ar.uade.cine.swing.api.dto.Clasificacion;
import ar.uade.cine.swing.api.dto.DeclaracionJurada;
import ar.uade.cine.swing.api.dto.Empleado;
import ar.uade.cine.swing.api.dto.Funcion;
import ar.uade.cine.swing.api.dto.InformeFuncion;
import ar.uade.cine.swing.api.dto.PedidoFuncion;
import ar.uade.cine.swing.api.dto.PedidoPelicula;
import ar.uade.cine.swing.api.dto.PedidoSala;
import ar.uade.cine.swing.api.dto.Pelicula;
import ar.uade.cine.swing.api.dto.Reserva;
import ar.uade.cine.swing.api.dto.Sala;
import ar.uade.cine.swing.api.dto.Tarifa;
import ar.uade.cine.swing.api.dto.TipoSala;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El equivalente de escritorio de {@code cine-frontend/src/api/api-http.js}: todo el acceso al backend pasa por acá,
 * y una operación nueva se agrega acá con el mismo nombre que tiene en el front. Es bloqueante a propósito:
 * quien llama la corre fuera del EDT (ver {@code comun.Tarea}), y así los tests la usan sin ventanas.
 */
public final class ApiHttp {

    public static final String URL_POR_DEFECTO = "http://localhost:8080";

    private final String base;
    private final HttpClient http;
    private final ObjectMapper json;
    // El backend no guarda sesión: cada pedido lleva Basic. Vive en memoria y se va al cerrar la app.
    private volatile String credenciales;
    private volatile Runnable alVencerSesion = () -> { };

    public ApiHttp(String urlBase) {
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

    public boolean tieneCredenciales() {
        return credenciales != null;
    }

    // --- sesión ---

    public Empleado login(String email, String password) {
        // Van las credenciales nuevas en Basic y también en el cuerpo: el backend de hoy valida el cuerpo (y rechaza
        // un Basic inválido), y el que valida con Spring Security mira solo el header. Así anda contra los dos.
        // Nunca las viejas: si vencieron, el filtro rechazaría el pedido antes de probar las nuevas.
        String nuevas = Base64.getEncoder()
                .encodeToString((nulo(email) + ":" + nulo(password)).getBytes(StandardCharsets.UTF_8));
        Empleado empleado = pedir("POST", "/sesion", Map.of("email", nulo(email), "password", nulo(password)),
                tipo(Empleado.class), nuevas, false);
        // Solo se guardan si el login contestó bien: un 401 sale por excepción antes de llegar acá.
        credenciales = nuevas;
        return empleado;
    }

    // --- catálogos ---

    public List<String> obtenerGeneros() {
        return lista("/generos", String.class);
    }

    public List<Clasificacion> obtenerClasificaciones() {
        return lista("/clasificaciones", Clasificacion.class);
    }

    public List<TipoSala> obtenerTiposSala() {
        return lista("/tipos-sala", TipoSala.class);
    }

    public List<Tarifa> obtenerTarifas() {
        return lista("/tarifas", Tarifa.class);
    }

    public List<String> obtenerIdiomas() {
        return lista("/idiomas", String.class);
    }

    public List<String> obtenerProyecciones() {
        return lista("/proyecciones", String.class);
    }

    // --- películas ---

    public List<Pelicula> obtenerPeliculas(Map<String, String> filtros) {
        return lista("/peliculas" + consulta(filtros), Pelicula.class);
    }

    public Pelicula crearPelicula(PedidoPelicula pelicula) {
        return pedir("POST", "/peliculas", pelicula, tipo(Pelicula.class));
    }

    public Pelicula actualizarPelicula(int id, PedidoPelicula cambios) {
        return pedir("PUT", "/peliculas/" + id, cambios, tipo(Pelicula.class));
    }

    public void eliminarPelicula(int id) {
        pedir("DELETE", "/peliculas/" + id, null, tipo(JsonNode.class));
    }

    // --- salas ---

    public List<Sala> obtenerSalas() {
        return lista("/salas", Sala.class);
    }

    public Sala crearSala(PedidoSala sala) {
        return pedir("POST", "/salas", sala, tipo(Sala.class));
    }

    public void eliminarSala(int id) {
        pedir("DELETE", "/salas/" + id, null, tipo(JsonNode.class));
    }

    // --- funciones e informes ---

    public List<Funcion> obtenerFunciones(Map<String, String> filtros) {
        return lista("/funciones" + consulta(filtros), Funcion.class);
    }

    public Funcion obtenerFuncion(int id) {
        return pedir("GET", "/funciones/" + id, null, tipo(Funcion.class));
    }

    public Funcion programarFuncion(PedidoFuncion funcion) {
        return pedir("POST", "/funciones", funcion, tipo(Funcion.class));
    }

    public void eliminarFuncion(int id) {
        pedir("DELETE", "/funciones/" + id, null, tipo(JsonNode.class));
    }

    public Bordero obtenerBordero(int funcionId) {
        return pedir("GET", "/funciones/" + funcionId + "/bordero", null, tipo(Bordero.class));
    }

    public InformeFuncion obtenerInformeDeFuncion(int funcionId) {
        return pedir("GET", "/funciones/" + funcionId + "/informe", null, tipo(InformeFuncion.class));
    }

    // --- caja y puerta ---

    public Arqueo obtenerArqueo(String fecha) {
        return pedir("GET", "/arqueo" + consulta(Map.of("fecha", nulo(fecha))), null, tipo(Arqueo.class));
    }

    public ArqueoCandy obtenerArqueoCandy(String fecha) {
        return pedir("GET", "/candy/arqueo" + consulta(Map.of("fecha", nulo(fecha))), null,
                tipo(ArqueoCandy.class));
    }

    /** Sin fechas, el backend devuelve la última semana cinematográfica cerrada. */
    public DeclaracionJurada obtenerDeclaracionJurada(String desde, String hasta) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("desde", desde);
        filtros.put("hasta", hasta);
        return pedir("GET", "/declaracion-jurada" + consulta(filtros), null, tipo(DeclaracionJurada.class));
    }

    public Reserva validarEntrada(String codigo) {
        return pedir("POST", "/acceso", Map.of("codigo", nulo(codigo)), tipo(Reserva.class));
    }

    // --- mecánica ---

    private <T> List<T> lista(String ruta, Class<T> elemento) {
        return pedir("GET", ruta, null, json.getTypeFactory().constructCollectionType(List.class, elemento));
    }

    private JavaType tipo(Class<?> clase) {
        return json.getTypeFactory().constructType(clase);
    }

    // Map.of no acepta null: un campo vacío viaja como "" y el backend lo rechaza con su propio mensaje.
    private static String nulo(String valor) {
        return valor == null ? "" : valor;
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

    private <T> T pedir(String metodo, String ruta, Object cuerpo, JavaType tipo) {
        return pedir(metodo, ruta, cuerpo, tipo, credenciales, true);
    }

    // `avisaVencida`: un 401 del login es "clave incorrecta", no una sesión que se cayó.
    private <T> T pedir(String metodo, String ruta, Object cuerpo, JavaType tipo, String clave,
                        boolean avisaVencida) {
        HttpResponse<byte[]> respuesta = enviar(metodo, ruta, cuerpo, clave, avisaVencida);
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
                                        boolean avisaVencida) {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(30));
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
            throw new ErrorApi(0, "No se pudo conectar con el servidor en " + base);
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

    private String mensajeDeError(int estado, byte[] cuerpo) {
        String texto = new String(cuerpo, StandardCharsets.UTF_8).strip();
        if (texto.isEmpty()) return "Error " + estado + " del servidor";
        try {
            JsonNode datos = json.readTree(texto);
            return datos.hasNonNull("error") ? datos.get("error").asText() : "Error " + estado + " del servidor";
        } catch (JsonProcessingException e) {
            // Un proxy caído contesta HTML o texto: se muestra eso antes que un "error de parseo".
            return texto;
        }
    }

    private String escribir(Object cuerpo) {
        try {
            return json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el JSON del pedido", e);
        }
    }
}
