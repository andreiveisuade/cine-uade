package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.dto.*;
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
    private static final Duration ESPERA = Duration.ofSeconds(30);
    // La importación contesta al terminar (10-15 s, hasta 120 s en el backend): la espera normal la cortaría.
    private static final Duration ESPERA_IMPORTACION = Duration.ofSeconds(180);

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

    public List<MedioPago> obtenerMediosPago() {
        return lista("/medios-pago", MedioPago.class);
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

    public List<Pelicula> obtenerPeliculasPendientes() {
        return lista("/peliculas/pendientes", Pelicula.class);
    }

    public Pelicula confirmarPelicula(int id) {
        return pedir("POST", "/peliculas/" + id + "/confirmacion", Map.of(), tipo(Pelicula.class));
    }

    public Pelicula descartarPelicula(int id) {
        return pedir("POST", "/peliculas/" + id + "/descarte", Map.of(), tipo(Pelicula.class));
    }

    public Importacion importarAhora(int paginas) {
        return pedir("POST", "/importaciones", Map.of("paginas", paginas), tipo(Importacion.class), credenciales,
                true, ESPERA_IMPORTACION);
    }

    public List<Importacion> obtenerImportaciones() {
        return lista("/importaciones", Importacion.class);
    }

    public EstadoImportador estadoImportador() {
        return pedir("GET", "/importaciones/estado", null, tipo(EstadoImportador.class));
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

    public Sala obtenerSala(int id) {
        return pedir("GET", "/salas/" + id, null, tipo(Sala.class));
    }

    public Sala crearSala(PedidoSala sala) {
        return pedir("POST", "/salas", sala, tipo(Sala.class));
    }

    public void eliminarSala(int id) {
        pedir("DELETE", "/salas/" + id, null, tipo(JsonNode.class));
    }

    public Asiento cambiarEstadoAsiento(int salaId, String codigo, String estado) {
        return pedir("PUT", "/salas/" + salaId + "/asientos/" + segmento(codigo.trim().toUpperCase()),
                Map.of("estado", nulo(estado)), tipo(Asiento.class));
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

    // --- programaciones y grilla ---

    public List<Programacion> obtenerProgramaciones(Map<String, String> filtros) {
        return lista("/programaciones" + consulta(filtros), Programacion.class);
    }

    public Programacion obtenerProgramacion(int id) {
        return pedir("GET", "/programaciones/" + id, null, tipo(Programacion.class));
    }

    public Plan previsualizarProgramacion(PedidoProgramacion programacion) {
        return pedir("POST", "/programaciones/previsualizar", programacion, tipo(Plan.class));
    }

    public Plan crearProgramacion(PedidoProgramacion programacion) {
        return pedir("POST", "/programaciones", programacion, tipo(Plan.class));
    }

    public Programacion cambiarActivacionProgramacion(int id, boolean activa) {
        return pedir("PATCH", "/programaciones/" + id, Map.of("activa", activa), tipo(Programacion.class));
    }

    public PropuestaGrilla proponerGrilla(PedidoGrilla criterios) {
        return pedir("POST", "/grilla/propuesta", criterios, tipo(PropuestaGrilla.class));
    }

    public PropuestaGrilla armarGrilla(PedidoGrilla criterios) {
        return pedir("POST", "/grilla", criterios, tipo(PropuestaGrilla.class));
    }

    // --- reservas y cobro ---

    public List<Reserva> obtenerReservas(Map<String, String> filtros) {
        return lista("/reservas" + consulta(filtros), Reserva.class);
    }

    public Reserva obtenerReserva(int id) {
        return pedir("GET", "/reservas/" + id, null, tipo(Reserva.class));
    }

    public Reserva cancelarReserva(int id) {
        return pedir("POST", "/reservas/" + id + "/cancelacion", null, tipo(Reserva.class));
    }

    /** El monto no viaja: el descuento depende del medio y lo resuelve el backend al cobrar. */
    public Pago cobrar(int reservaId, String medio, String codigoAutorizacion) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("medio", medio);
        cuerpo.put("codigoAutorizacion", codigoAutorizacion);
        return pedir("POST", "/reservas/" + reservaId + "/pago", cuerpo, tipo(Pago.class));
    }

    public Pago obtenerPagoDeReserva(int reservaId) {
        return pedir("GET", "/reservas/" + reservaId + "/pago", null, tipo(Pago.class));
    }

    public Checkout abrirCheckout(int reservaId, String medio) {
        return pedir("POST", "/reservas/" + reservaId + "/checkout", Map.of("medio", nulo(medio)),
                tipo(Checkout.class));
    }

    public Pago confirmarCheckout(String checkoutId) {
        return pedir("POST", "/checkouts/" + segmento(checkoutId) + "/confirmacion", null, tipo(Pago.class));
    }

    // --- promociones ---

    public List<Promocion> obtenerPromociones() {
        return lista("/promociones", Promocion.class);
    }

    public Promocion crearPromocion(PedidoPromocion promocion) {
        return pedir("POST", "/promociones", promocion, tipo(Promocion.class));
    }

    public Promocion cambiarActivacionPromocion(int id, boolean activa) {
        return pedir("PATCH", "/promociones/" + id, Map.of("activa", activa), tipo(Promocion.class));
    }

    // --- candy ---

    public List<Producto> obtenerProductosCandy(boolean todos) {
        return lista("/candy/productos" + (todos ? "?todos=true" : ""), Producto.class);
    }

    public Producto crearProductoCandy(PedidoProducto producto) {
        return pedir("POST", "/candy/productos", producto, tipo(Producto.class));
    }

    public Producto armarComboCandy(PedidoCombo combo) {
        return pedir("POST", "/candy/combos", combo, tipo(Producto.class));
    }

    public Producto editarProductoCandy(int id, String nombre, Double precio) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombre", nombre);
        cuerpo.put("precio", precio);
        return pedir("PUT", "/candy/productos/" + id, cuerpo, tipo(Producto.class));
    }

    public Producto cambiarDisponibilidadCandy(int id, boolean disponible) {
        return pedir("PUT", "/candy/productos/" + id + "/disponibilidad", Map.of("disponible", disponible),
                tipo(Producto.class));
    }

    public CompraCandy venderCandy(PedidoVenta venta) {
        return pedir("POST", "/candy/compras", venta, tipo(CompraCandy.class));
    }

    public List<CompraCandy> obtenerComprasCandy(Map<String, String> filtros) {
        return lista("/candy/compras" + consulta(filtros), CompraCandy.class);
    }

    public Cliente buscarClientePorEmail(String email) {
        return pedir("GET", "/clientes" + consulta(Map.of("email", nulo(email))), null, tipo(Cliente.class));
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

    // Un código de butaca o de checkout va en la ruta: con URLEncoder un espacio sería "+", que ahí no es espacio.
    private static String segmento(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
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

    private <T> T pedir(String metodo, String ruta, Object cuerpo, JavaType tipo, String clave,
                        boolean avisaVencida) {
        return pedir(metodo, ruta, cuerpo, tipo, clave, avisaVencida, ESPERA);
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
