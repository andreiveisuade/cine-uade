package ar.uade.cine.infrastructure.importador.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.infrastructure.importador.ImportadorError;
import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * El cliente de TMDB, contra un servidor de mentira que contesta lo que el test quiera.
 *
 * <p>Es la única pieza del circuito que los tests de ruta no ven: {@code ApiEnMemoria} usa un
 * {@code CatalogoDePrueba} y nunca sale por HTTP. Lo que se prueba acá es que las tres
 * llamadas se compongan bien y que un error de TMDB se convierta en un mensaje que se le
 * pueda mostrar al encargado.
 *
 * <p>Con {@code com.sun.net.httpserver} del JDK y no con un mock de {@code HttpClient}: así se
 * ejerce el pedido de verdad, códigos de estado incluidos. Puerto 0, que lo elige el sistema
 * operativo, por lo mismo que {@code ApiEnMemoria}.
 */
class TmdbHttpTest {

    private HttpServer servidor;

    @AfterEach
    void bajarElServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void componeLasTresLlamadasEnUnaPelicula() {
        levantar(intercambio -> {
            switch (intercambio.getRequestURI().getPath()) {
                case "/movie/now_playing" -> responder(intercambio, 200, """
                        {"results": [{"id": 42, "title": "Duna", "poster_path": "/duna.jpg"}],
                         "total_pages": 1}""");
                case "/movie/42" -> responder(intercambio, 200, """
                        {"title": "Duna", "runtime": 166, "original_language": "en",
                         "release_date": "2024-02-28", "vote_average": 8.2, "vote_count": 5400,
                         "genres": [{"name": "Ciencia ficción"}]}""");
                case "/movie/42/release_dates" -> responder(intercambio, 200, """
                        {"results": [{"iso_3166_1": "US", "release_dates": [{"certification": "PG-13"}]},
                                     {"iso_3166_1": "AR", "release_dates": [{"certification": "+13"}]}]}""");
                default -> responder(intercambio, 404, "{}");
            }
        });

        List<DatosPelicula> peliculas = catalogo().enCartelera(1);

        assertEquals(1, peliculas.size());
        DatosPelicula duna = peliculas.get(0);
        assertEquals("Duna", duna.titulo());
        assertEquals(166, duna.duracionMinutos());
        assertEquals(List.of(Genero.CIENCIA_FICCION), duna.generos());
        // La argentina, no la primera que aparezca: PG-13 es de Estados Unidos.
        assertEquals(Clasificacion.MAS_13, duna.clasificacion());
        assertEquals("https://image.tmdb.org/t/p/w500/duna.jpg", duna.posterUrl());
    }

    /** El idioma va en toda llamada: es lo que hace que los géneros vuelvan en castellano. */
    @Test
    void elIdiomaYLaRegionViajanEnLaConsulta() {
        StringBuilder recibido = new StringBuilder();
        levantar(intercambio -> {
            recibido.append(intercambio.getRequestURI().getQuery()).append(' ');
            responder(intercambio, 200, "{\"results\": [], \"total_pages\": 1}");
        });

        catalogo().enCartelera(1);

        assertTrue(recibido.toString().contains("language=es-AR"), recibido.toString());
        assertTrue(recibido.toString().contains("region=AR"), recibido.toString());
    }

    /**
     * Si TMDB falla en una película puntual, la película sale igual y sin duración: el alta la
     * va a rechazar y va a quedar nombrada en el detalle, en vez de desaparecer del reporte y
     * dejar al encargado creyendo que TMDB trajo menos títulos de los que trajo.
     */
    @Test
    void unErrorEnUnaPeliculaNoTiraLaCorrida() {
        levantar(intercambio -> {
            if (intercambio.getRequestURI().getPath().equals("/movie/now_playing")) {
                responder(intercambio, 200, """
                        {"results": [{"id": 1, "title": "La que falla"}], "total_pages": 1}""");
            } else {
                responder(intercambio, 500, "{}");
            }
        });

        List<DatosPelicula> peliculas = catalogo().enCartelera(1);

        assertEquals(1, peliculas.size());
        assertEquals("La que falla", peliculas.get(0).titulo());
        assertEquals(0, peliculas.get(0).duracionMinutos());
    }

    /** Que el listado entero falle sí es una corrida fallida, y el mensaje se le muestra. */
    @Test
    void elTokenRechazadoSeDiceConLoQueHayQueHacer() {
        levantar(intercambio -> responder(intercambio, 401, "{}"));

        ImportadorError error = assertThrows(ImportadorError.class, () -> catalogo().enCartelera(1));

        assertEquals("TMDB rechazó el token: revisá TMDB_TOKEN", error.getMessage());
    }

    @Test
    void sinTokenNiSiquieraSaleAPedir() {
        levantar(intercambio -> responder(intercambio, 200, "{}"));
        TmdbHttp sinToken = new TmdbHttp(null, "AR", direccion());

        assertFalse(sinToken.consultar().disponible());
        assertTrue(assertThrows(ImportadorError.class, () -> sinToken.enCartelera(1))
                .getMessage().startsWith("Falta el token de TMDB"));
    }

    @Test
    void conTokenElCatalogoSeDeclaraListo() {
        assertTrue(new TmdbHttp("un-token", "AR", "http://tmdb").consultar().disponible());
    }

    private TmdbHttp catalogo() {
        return new TmdbHttp("un-token", "AR", direccion());
    }

    private void levantar(Consumer<HttpExchange> manejador) {
        try {
            servidor = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", manejador::accept);
        servidor.start();
    }

    private String direccion() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    private static void responder(HttpExchange intercambio, int codigo, String cuerpo) {
        byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
        try (OutputStream salida = intercambio.getResponseBody()) {
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(codigo, datos.length);
            salida.write(datos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
