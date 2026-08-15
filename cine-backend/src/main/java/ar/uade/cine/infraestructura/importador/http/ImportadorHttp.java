package ar.uade.cine.infraestructura.importador.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.uade.cine.infraestructura.importador.ImportadorCartelera;
import ar.uade.cine.infraestructura.importador.ImportadorError;

/**
 * El importador de verdad: el contenedor {@code parser}, al otro lado de la red interna.
 *
 * <p>Es la <strong>única</strong> llamada saliente de todo el backend, y por eso vale la
 * pena decir qué no es: no sale a internet. {@code parser} es un nombre que solo resuelve
 * adentro de Docker, en la red {@code web}, la misma por la que nginx llega hasta acá. El
 * que sale a internet —a TMDB— es el importador, que para eso está solo en esa red y no
 * tiene ruta hasta MySQL: todo lo que trae entra por la API y pasa por las reglas.
 *
 * <p>La dirección viene de {@code IMPORTADOR_URL} por lo mismo que la base de datos viene
 * de {@code DB_HOST}: dónde está el importador es una decisión del despliegue, no del
 * código. Sin la variable asume el nombre del servicio en el compose.
 */
public class ImportadorHttp implements ImportadorCartelera {

    private static final String DIRECCION_POR_DEFECTO = "http://parser:8090";

    /**
     * Dos minutos, aunque una corrida tarde diez segundos. No es para el caso normal: es
     * para que un TMDB lento no deje el pedido colgado hasta que se aburra el navegador.
     * nginx corta esa misma ruta a los tres minutos, así que el que corta primero es este
     * y el encargado recibe un mensaje escrito en castellano en vez de un 504.
     */
    private static final Duration ESPERA_CORRIDA = Duration.ofMinutes(2);

    /** El latido tiene que ser barato: si no contesta en dos segundos, no está. */
    private static final Duration ESPERA_SALUD = Duration.ofSeconds(2);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ImportadorHttp() {
        this(direccionConfigurada());
    }

    public ImportadorHttp(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public Resumen importar(int paginas) {
        HttpResponse<String> respuesta = pedir(
                HttpRequest.newBuilder(URI.create(base + "/importar"))
                        .header("Content-Type", "application/json")
                        .timeout(ESPERA_CORRIDA)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"paginas\": " + paginas + "}")));

        JsonNode cuerpo = leer(respuesta);
        if (respuesta.statusCode() != 200) {
            // El importador manda {"error": "..."} con el motivo ya redactado —le falta el
            // token, ya está corriendo—: se propaga tal cual, como los 400 de los gestores.
            throw new ImportadorError(cuerpo.path("error").asText(
                    "El importador respondió " + respuesta.statusCode()));
        }
        return new Resumen(
                cuerpo.path("nuevas").asInt(),
                cuerpo.path("salteadas").asInt(),
                cuerpo.path("fallidas").asInt(),
                cuerpo.path("segundos").asDouble(),
                detalleDe(cuerpo));
    }

    /**
     * Nunca tira: la pregunta es justamente si está, y "no está" es una respuesta. El texto
     * lleva el comando para levantarlo porque es lo que va a leer el encargado en pantalla.
     */
    @Override
    public Estado consultar() {
        try {
            HttpResponse<String> respuesta = pedir(
                    HttpRequest.newBuilder(URI.create(base + "/salud"))
                            .timeout(ESPERA_SALUD)
                            .GET());
            if (respuesta.statusCode() != 200) {
                return new Estado(false, "El importador respondió " + respuesta.statusCode());
            }
            JsonNode cuerpo = leer(respuesta);
            if (!cuerpo.path("tokenConfigurado").asBoolean(true)) {
                return new Estado(false, "Al importador le falta el token de TMDB: "
                        + "cargá TMDB_TOKEN en el .env y reiniciá el contenedor");
            }
            return new Estado(true, cuerpo.path("corriendo").asBoolean()
                    ? "Hay una corrida en curso" : "Listo para traer cartelera");
        } catch (ImportadorError e) {
            return new Estado(false, e.getMessage());
        }
    }

    /** Las líneas del log de la corrida, que el importador manda como lista. */
    private static String detalleDe(JsonNode cuerpo) {
        JsonNode lineas = cuerpo.path("detalle");
        if (!lineas.isArray() || lineas.isEmpty()) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (JsonNode linea : lineas) {
            texto.append(linea.asText()).append('\n');
        }
        return texto.toString().strip();
    }

    private HttpResponse<String> pedir(HttpRequest.Builder pedido) {
        try {
            return cliente.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ImportadorError("El importador no responde en " + base
                    + ": revisá que el contenedor esté levantado", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportadorError("Se interrumpió la importación", e);
        }
    }

    private static JsonNode leer(HttpResponse<String> respuesta) {
        try {
            return JSON.readTree(respuesta.body());
        } catch (IOException e) {
            throw new ImportadorError("El importador contestó algo que no es JSON", e);
        }
    }

    private static String direccionConfigurada() {
        String url = System.getenv("IMPORTADOR_URL");
        return url == null || url.isBlank() ? DIRECCION_POR_DEFECTO : url;
    }
}
