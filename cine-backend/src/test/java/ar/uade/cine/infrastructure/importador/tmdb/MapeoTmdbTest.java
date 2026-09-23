package ar.uade.cine.infrastructure.importador.tmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import org.junit.jupiter.api.Test;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

/**
 * Lo caro si se rompe en silencio: que un género ajeno caiga siempre en uno nuestro y que la
 * falta de datos se resuelva del lado prudente.
 */
class MapeoTmdbTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void variosGenerosAjenosCaenEnElMismoNuestroYNoSeRepite() {
        JsonNode detalle = json("""
                {"genres": [{"name": "Acción"}, {"name": "Aventura"}, {"name": "Bélica"}]}""");

        assertEquals(List.of(Genero.ACCION), MapeoTmdb.generosDe(detalle));
    }

    /** La pérdida es a propósito: Vaiana entra como acción y ciencia ficción. */
    @Test
    void fantasiaCaeEnCienciaFiccion() {
        JsonNode detalle = json("""
                {"genres": [{"name": "Fantasía"}, {"name": "Aventura"}]}""");

        assertEquals(List.of(Genero.ACCION, Genero.CIENCIA_FICCION), MapeoTmdb.generosDe(detalle));
    }

    /** R7 exige al menos uno: DRAMA es el menos comprometido. */
    @Test
    void sinNingunGeneroReconocibleQuedaDrama() {
        assertEquals(List.of(Genero.DRAMA), MapeoTmdb.generosDe(json("""
                {"genres": [{"name": "Telenovela venusina"}]}""")));
        assertEquals(List.of(Genero.DRAMA), MapeoTmdb.generosDe(json("{}")));
    }

    /**
     * Una clasificación permisiva inventada dejaría entrar a un menor; el error prudente se
     * corrige a mano.
     */
    @Test
    void sinCertificacionArgentinaElDefaultEsElRestrictivo() {
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe(null));
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe(""));
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe("no la publicaron"));
    }

    /** El INCAA no publica siempre con la misma forma, y TMDB copia lo que le dan. */
    @Test
    void lasTresFormasDeLaMismaCertificacion() {
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe("+13"));
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe("13"));
        assertEquals(Clasificacion.MAS_13, MapeoTmdb.clasificacionDe("sam13"));
        assertEquals(Clasificacion.ATP, MapeoTmdb.clasificacionDe(" atp "));
        // Condicionada: la más restrictiva que publica el INCAA.
        assertEquals(Clasificacion.MAS_18, MapeoTmdb.clasificacionDe("C"));
    }

    @Test
    void elIdiomaSeTraduceYSiNoEstaEnLaTablaQuedaElCodigo() {
        assertEquals("Inglés", MapeoTmdb.idiomaDe("en"));
        assertEquals("Coreano", MapeoTmdb.idiomaDe("ko"));
        assertEquals("sv", MapeoTmdb.idiomaDe("sv"));
    }

    @Test
    void elAnioSaleDeLaFechaDeEstrenoYCeroSiNoSeEntiende() {
        assertEquals(2026, MapeoTmdb.anioDe("2026-08-15"));
        assertEquals(0, MapeoTmdb.anioDe(null));
        assertEquals(0, MapeoTmdb.anioDe(""));
        assertEquals(0, MapeoTmdb.anioDe("proximamente"));
    }

    @Test
    void laPeliculaCompletaLlegaConLosNombresNuestros() {
        JsonNode resumen = json("""
                {"id": 1, "title": "Duna: Parte dos", "poster_path": "/duna.jpg"}""");
        JsonNode detalle = json("""
                {"title": "Duna: Parte dos", "runtime": 166, "overview": "Paul y los Fremen",
                 "release_date": "2024-02-28", "original_language": "en",
                 "vote_average": 8.2, "vote_count": 5400,
                 "genres": [{"name": "Ciencia ficción"}, {"name": "Aventura"}]}""");

        DatosPelicula pelicula = MapeoTmdb.aPelicula(resumen, detalle, "+13", "http://poster");

        assertEquals("Duna: Parte dos", pelicula.titulo());
        assertEquals(166, pelicula.duracionMinutos());
        assertEquals(List.of(Genero.ACCION, Genero.CIENCIA_FICCION), pelicula.generos());
        assertEquals(Clasificacion.MAS_13, pelicula.clasificacion());
        assertEquals("Paul y los Fremen", pelicula.sinopsis());
        assertEquals(2024, pelicula.anio());
        assertEquals("Inglés", pelicula.idiomaOriginal());
        assertEquals(8.2, pelicula.puntaje());
        assertEquals(5400, pelicula.votos());
        assertEquals("http://poster", pelicula.posterUrl());
    }

    /** Si cambia el buzón, una película que nadie miró no puede ofrecerse sola. */
    @Test
    void laPeliculaNaceFueraDeCartelera() {
        DatosPelicula pelicula = MapeoTmdb.aPelicula(json("{}"), json("""
                {"title": "Duna", "runtime": 166}"""), "ATP", "");

        assertFalse(pelicula.enCartelera());
    }

    /** El alta la rechaza por R2 y así queda nombrada en el detalle de la corrida. */
    @Test
    void sinDetalleQuedaElTituloDelListadoYSinDuracion() {
        JsonNode resumen = json("""
                {"id": 7, "title": "Una que TMDB no completó"}""");

        DatosPelicula pelicula = MapeoTmdb.aPelicula(
                resumen, MissingNode.getInstance(), null, "");

        assertEquals("Una que TMDB no completó", pelicula.titulo());
        assertEquals(0, pelicula.duracionMinutos());
        assertEquals(Clasificacion.MAS_13, pelicula.clasificacion());
        assertEquals(List.of(Genero.DRAMA), pelicula.generos());
    }

    private static JsonNode json(String texto) {
        try {
            return JSON.readTree(texto);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
