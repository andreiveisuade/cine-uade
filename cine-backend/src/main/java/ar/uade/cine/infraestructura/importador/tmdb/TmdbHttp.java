package ar.uade.cine.infraestructura.importador.tmdb;

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

import ar.uade.cine.infraestructura.importador.CatalogoExterno;
import ar.uade.cine.infraestructura.importador.ImportadorError;
import ar.uade.cine.servicio.cartelera.DatosPelicula;

/**
 * TMDB, acotado a lo que el cine necesita.
 *
 * <p>Son tres llamadas por película, porque TMDB reparte el dato en tres recursos:
 *
 * <pre>
 * /movie/now_playing?region=AR   qué se está dando en Argentina
 * /movie/{id}                    duración y géneros con nombre
 * /movie/{id}/release_dates      la clasificación por edad argentina
 * </pre>
 *
 * <p>Esa separación es una decisión de diseño de ellos: cada recurso trae una cosa y el
 * cliente compone. Nuestra API hace lo contrario —{@code GET /funciones/{id}} ya trae sala,
 * película y butacas— porque el frontend dibuja el mapa de una sola llamada. Ninguna de las
 * dos está mal: TMDB no puede saber qué combinación necesita cada uno de sus miles de
 * clientes, y nosotros sí sabemos qué necesita el nuestro.
 *
 * <p>Es la <strong>única</strong> llamada saliente del backend. Antes tampoco salía a
 * internet —le pedía la corrida a un proceso Python que era el que hablaba con TMDB—, y esa
 * indirección se eliminó: lo que el proceso aparte traducía es una decisión nuestra y no
 * tenía por qué vivir afuera del sistema.
 */
public class TmdbHttp implements CatalogoExterno {

    private static final String BASE_POR_DEFECTO = "https://api.themoviedb.org/3";
    private static final String IMAGENES = "https://image.tmdb.org/t/p/w500";

    /**
     * Cuántas películas se completan en paralelo. Son dos llamadas a TMDB por película y cada
     * una duerme {@link #PAUSA_ENTRE_LLAMADAS} al terminar: de a una, veinte películas son
     * casi un minuto de espera pura, y el encargado que aprieta el botón lo espera mirando la
     * pantalla. Cuatro lo dejan en diez o quince segundos sin acercarse al límite de TMDB.
     */
    private static final int HILOS = 4;

    /**
     * Sin tocar: TMDB pide una pausa entre llamadas y hacemos tres por película.
     *
     * <p>La pausa es por hilo, no global: cada llamada duerme la suya al terminar. Con cuatro
     * hilos el ritmo real es de unas trece llamadas por segundo, muy por debajo de lo que TMDB
     * acepta. Subir los hilos sin subir esta pausa es lo que empieza a devolver 429.
     */
    private static final Duration PAUSA_ENTRE_LLAMADAS = Duration.ofMillis(300);

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Lo que se usa como detalle cuando TMDB no lo pudo dar. Ver {@link #completar}. */
    private static final JsonNode SIN_DETALLE = MissingNode.getInstance();

    private final String token;
    private final String region;
    private final String base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Como corre en producción: el token y la región salen del entorno. */
    public TmdbHttp() {
        this(System.getenv("TMDB_TOKEN"), variable("TMDB_REGION", "AR"), BASE_POR_DEFECTO);
    }

    /**
     * La dirección entra por constructor para poder probar el cliente contra un servidor de
     * mentira, sin token de verdad y sin gastar cuota.
     */
    public TmdbHttp(String token, String region, String base) {
        this.token = token;
        this.region = region;
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Override
    public List<DatosPelicula> enCartelera(int paginas) {
        exigirToken();
        List<JsonNode> resumenes = buscarEnCartelera(paginas);

        // El cierre del try-with-resources espera a que terminen todas: no hace falta un
        // awaitTermination a mano.
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
        // No se le pega a TMDB para responder esto: la pantalla lo pregunta cada vez que se
        // abre y sería gastar cuota para contestar algo que ya sabemos.
        return new Estado(true, "Listo para traer cartelera");
    }

    /** Las películas que hoy están en cines argentinos, sin completar. */
    private List<JsonNode> buscarEnCartelera(int paginas) {
        List<JsonNode> resumenes = new ArrayList<>();
        for (int pagina = 1; pagina <= paginas; pagina++) {
            JsonNode datos = pedir("/movie/now_playing",
                    "region", region, "page", String.valueOf(pagina));
            datos.path("results").forEach(resumenes::add);
            // TMDB pagina: pedir de más no rompe, pero tampoco trae nada nuevo.
            if (pagina >= datos.path("total_pages").asInt(1)) {
                break;
            }
        }
        return resumenes;
    }

    /**
     * Las dos llamadas que faltan para una película: duración y géneros por un lado, la
     * clasificación argentina por el otro. Es lo único que se paraleliza.
     *
     * <p>Si TMDB falla en una película puntual —un 429, un 500— la película <strong>se
     * devuelve igual</strong>, con lo que trajo el listado y sin duración. No es un descuido:
     * así el gestor la rechaza por R2 y queda contada y nombrada en el detalle de la corrida,
     * en vez de desaparecer del reporte y dejar al encargado creyendo que TMDB tenía menos
     * títulos de los que tenía. El motivo real queda en el log del backend.
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

    /**
     * La clasificación por edad tal como la publica el INCAA: ATP, 13, 16, 18.
     *
     * <p>TMDB la modela como par (país, certificación) porque cada país tiene la suya.
     * Nosotros la tenemos como enum argentino fijo, que es más simple y alcanza para un cine
     * que opera en un solo país.
     */
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

    /** El idioma va en todas: es lo que hace que los géneros vuelvan en castellano. */
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

    /**
     * Junta lo que trajo cada hilo. Si uno se rompió por algo que no es un
     * {@link ImportadorError} —quedarse sin memoria, un bug del mapeo— la corrida entera
     * falla: eso no es «TMDB anda mal», es el importador roto, y esconderlo dejaría corridas
     * a medias sin que nadie se entere.
     */
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
