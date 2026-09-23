package ar.uade.cine.infrastructure.importador.tmdb;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import ar.uade.cine.infrastructure.importador.CatalogoExterno;
import ar.uade.cine.infrastructure.importador.ImportadorError;
import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * Cliente de TMDB, la única llamada saliente del backend. TMDB reparte el dato en tres
 * recursos:
 *
 * <pre>
 * /movie/now_playing?region=AR   qué se está dando en Argentina
 * /movie/{id}                    duración y géneros con nombre
 * /movie/{id}/release_dates      la clasificación por edad argentina
 * </pre>
 */
public class TmdbHttp implements CatalogoExterno {

    private static final String BASE_POR_DEFECTO = "https://api.themoviedb.org/3";
    private static final String IMAGENES = "https://image.tmdb.org/t/p/w500";

    /** De a una, veinte películas son casi un minuto de pausas; cuatro no rozan el límite de TMDB. */
    private static final int HILOS = 4;

    /** Pausa por hilo, no global: subir {@link #HILOS} sin subir esto termina en 429. */
    private static final Duration PAUSA_ENTRE_LLAMADAS = Duration.ofMillis(300);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final JsonNode SIN_DETALLE = MissingNode.getInstance();

    private final String token;
    private final String region;
    private final String base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public TmdbHttp() {
        this(System.getenv("TMDB_TOKEN"), variable("TMDB_REGION", "AR"), BASE_POR_DEFECTO);
    }

    /** La base entra por parámetro para probar contra un servidor falso sin gastar cuota. */
    public TmdbHttp(String token, String region, String base) {
        this.token = token;
        this.region = region;
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public List<DatosPelicula> enCartelera(int paginas) {
        exigirToken();
        List<JsonNode> resumenes = buscarEnCartelera(paginas);

        // El cierre del try-with-resources espera a que terminen todas.
        try (ExecutorService hilos = Executors.newFixedThreadPool(HILOS)) {
            List<Future<DatosPelicula>> pedidos = new ArrayList<>();
            for (JsonNode resumen : resumenes) {
                pedidos.add(hilos.submit(() -> completar(resumen)));
            }
            return esperar(pedidos);
        }
    }

    @Override
    public Estado consultar() {
        if (token == null || token.isBlank()) {
            return new Estado(false, "Falta el token de TMDB: cargá TMDB_TOKEN en el .env "
                    + "y reiniciá el backend");
        }
        // Sin llamar a TMDB: la pantalla pregunta cada vez que se abre.
        return new Estado(true, "Listo para traer cartelera");
    }

    private List<JsonNode> buscarEnCartelera(int paginas) {
        List<JsonNode> resumenes = new ArrayList<>();
        for (int pagina = 1; pagina <= paginas; pagina++) {
            JsonNode datos = pedir("/movie/now_playing",
                    "region", region, "page", String.valueOf(pagina));
            datos.path("results").forEach(resumenes::add);
            if (pagina >= datos.path("total_pages").asInt(1)) {
                break;
            }
        }
        return resumenes;
    }

    /**
     * Si TMDB falla en una película se devuelve igual, sin duración: el gestor la rechaza por
     * R2 y queda contada en el detalle de la corrida en vez de desaparecer.
     */
    private DatosPelicula completar(JsonNode resumen) {
        int id = resumen.path("id").asInt();
        JsonNode detalle = SIN_DETALLE;
        String certificacion = null;
        try {
            detalle = pedir("/movie/" + id);
            certificacion = certificacionArgentina(id);
        } catch (ImportadorError e) {
            System.err.println("TMDB no pudo completar «"
                    + resumen.path("title").asText("?") + "»: " + e.getMessage());
        }
        return MapeoTmdb.aPelicula(resumen, detalle, certificacion,
                urlPoster(resumen.path("poster_path").asText(null)));
    }

    private String certificacionArgentina(int id) {
        JsonNode datos = pedir("/movie/" + id + "/release_dates");
        for (JsonNode pais : datos.path("results")) {
            if (!"AR".equals(pais.path("iso_3166_1").asText())) {
                continue;
            }
            for (JsonNode estreno : pais.path("release_dates")) {
                String certificacion = estreno.path("certification").asText("");
                if (!certificacion.isBlank()) {
                    return certificacion.strip();
                }
            }
        }
        return null;
    }

    private static String urlPoster(String posterPath) {
        return posterPath == null || posterPath.isBlank() ? "" : IMAGENES + posterPath;
    }

    private JsonNode pedir(String ruta, String... parametros) {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(base + ruta + consulta(parametros)))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        try {
            HttpResponse<String> respuesta =
                    cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 401) {
                throw new ImportadorError("TMDB rechazó el token: revisá TMDB_TOKEN");
            }
            if (respuesta.statusCode() != 200) {
                throw new ImportadorError("TMDB devolvió " + respuesta.statusCode()
                        + " en " + ruta);
            }
            return JSON.readTree(respuesta.body());
        } catch (IOException e) {
            throw new ImportadorError("No se pudo llegar a TMDB: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImportadorError("Se interrumpió la consulta a TMDB", e);
        } finally {
            pausar();
        }
    }

    /** El idioma va en todas para que los géneros vuelvan en castellano. */
    private String consulta(String... parametros) {
        StringBuilder url = new StringBuilder("?language=es-AR");
        for (int i = 0; i < parametros.length; i += 2) {
            url.append('&').append(parametros[i]).append('=')
                    .append(URLEncoder.encode(parametros[i + 1], StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private static void pausar() {
        try {
            Thread.sleep(PAUSA_ENTRE_LLAMADAS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Un fallo que no es {@link ImportadorError} es un bug y tira la corrida entera. */
    private static List<DatosPelicula> esperar(List<Future<DatosPelicula>> pedidos) {
        List<DatosPelicula> peliculas = new ArrayList<>();
        for (Future<DatosPelicula> pedido : pedidos) {
            try {
                peliculas.add(pedido.get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ImportadorError error) {
                    throw error;
                }
                throw new ImportadorError("El importador se rompió: " + e.getCause(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ImportadorError("Se interrumpió la importación", e);
            }
        }
        return peliculas;
    }

    private void exigirToken() {
        if (token == null || token.isBlank()) {
            throw new ImportadorError(
                    "Falta el token de TMDB. Se saca gratis en themoviedb.org/settings/api "
                    + "y se carga en TMDB_TOKEN, en el .env del compose.");
        }
    }

    private static String variable(String nombre, String siNoEsta) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? siNoEsta : valor;
    }
}
