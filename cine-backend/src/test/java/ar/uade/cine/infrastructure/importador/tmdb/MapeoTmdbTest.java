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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;

class MapeoTmdbTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void variosGenerosAjenosCaenEnElMismoNuestroYNoSeRepite() {
        JsonNode detalle = json("""
                {"genres": [{"name": "Acción"}, {"name": "Aventura"}, {"name": "Bélica"}]}""");

        assertEquals(List.of(Genero.ACCION), MapeoTmdb.generosDe(detalle));
    }

    @Test
    void fantasiaCaeEnCienciaFiccion() {
        JsonNode detalle = json("""
                {"genres": [{"name": "Fantasía"}, {"name": "Aventura"}]}""");

        assertEquals(List.of(Genero.ACCION, Genero.CIENCIA_FICCION), MapeoTmdb.generosDe(detalle));
    }

    @Test
    void sinNingunGeneroReconocibleQuedaDrama() {
        assertEquals(List.of(Genero.DRAMA), MapeoTmdb.generosDe(json("""
                {"genres": [{"name": "Telenovela venusina"}]}""")));
        assertEquals(List.of(Genero.DRAMA), MapeoTmdb.generosDe(json("{}")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin certificación argentina,  ,                 MAS_13
            certificación vacía,          '',               MAS_13
            una que no se entiende,       no la publicaron, MAS_13
            con el signo,                 +13,              MAS_13
            solo el número,               13,               MAS_13
            con el prefijo sam,           sam13,            MAS_13
            con espacios y en minúsculas, ' atp ',          ATP
            la letra C,                   C,                MAS_18
            """)
    void laCertificacionSeLeeEnSusVariasFormasYSinEllaEsLaRestrictiva(String caso, String certificacion,
            Clasificacion esperada) {
        assertEquals(esperada, MapeoTmdb.clasificacionDe(certificacion));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            inglés,                            en, Inglés
            coreano,                           ko, Coreano
            fuera de la tabla queda el código, sv, sv
            """)
    void elIdiomaSeTraduceYSiNoEstaEnLaTablaQuedaElCodigo(String caso, String codigo, String idioma) {
        assertEquals(idioma, MapeoTmdb.idiomaDe(codigo));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            de la fecha de estreno,       2026-08-15,   2026
            sin fecha,                    ,             0
            fecha vacía,                  '',           0
            una fecha que no se entiende, proximamente, 0
            """)
    void elAnioSaleDeLaFechaDeEstrenoYCeroSiNoSeEntiende(String caso, String fecha, int anio) {
        assertEquals(anio, MapeoTmdb.anioDe(fecha));
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

    @Test
    void laPeliculaNaceFueraDeCartelera() {
        DatosPelicula pelicula = MapeoTmdb.aPelicula(json("{}"), json("""
                {"title": "Duna", "runtime": 166}"""), "ATP", "");

        assertFalse(pelicula.enCartelera());
    }

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
