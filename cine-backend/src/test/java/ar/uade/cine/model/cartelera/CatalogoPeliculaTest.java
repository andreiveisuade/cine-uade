package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class CatalogoPeliculaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 13);

    private static final CatalogoPelicula VACIO = new CatalogoPelicula();

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(DatoInvalido.class, accion).getMessage());
    }

    private static CatalogoPelicula conPuntaje(double puntaje) {
        return VACIO.conCambios(null, null, null, null, null, puntaje, null, HOY);
    }

    private static CatalogoPelicula conVotos(int votos) {
        return VACIO.conCambios(null, null, null, null, null, null, votos, HOY);
    }

    private static CatalogoPelicula conAnio(int anio) {
        return VACIO.conCambios(null, null, anio, null, null, null, null, HOY);
    }

    private static CatalogoPelicula conPoster(String url) {
        return VACIO.conCambios(null, null, null, null, url, null, null, HOY);
    }

    @Test
    void naceVacio() {
        assertEquals("", VACIO.director());
        assertEquals("", VACIO.sinopsis());
        assertEquals(0, VACIO.anio());
        assertEquals("", VACIO.idiomaOriginal());
        assertEquals("", VACIO.posterUrl());
        assertEquals(0, VACIO.puntaje());
        assertEquals(0, VACIO.votos());
    }

    @Test
    void conCambiosSoloPisaLoQueVinoYNoTocaElOriginal() {
        CatalogoPelicula dune = VACIO.conCambios("Denis Villeneuve", "Arrakis", 2021, "Inglés",
                "https://poster.jpg", 8.1, 1200, HOY);

        CatalogoPelicula editado = dune.conCambios(null, "Otra sinopsis", null, null, null, null, null, HOY);

        assertNotSame(dune, editado);
        assertEquals("Arrakis", dune.sinopsis());
        assertEquals(VACIO.conCambios("Denis Villeneuve", "Otra sinopsis", 2021, "Inglés",
                "https://poster.jpg", 8.1, 1200, HOY), editado);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            negativo,           -0.1,  El puntaje tiene que estar entre 0 y 10
            más de diez,        10.1,  El puntaje tiene que estar entre 0 y 10
            dos decimales,      8.15,  El puntaje tiene que tener como máximo un decimal
            no es un número,    NaN,   El puntaje tiene que estar entre 0 y 10
            infinito,           Infinity, El puntaje tiene que estar entre 0 y 10
            """)
    void elPuntajeVaDeCeroADiezConUnDecimal(String caso, double puntaje, String mensaje) {
        rechaza(mensaje, () -> conPuntaje(puntaje));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 7.5, 10})
    void losBordesDelPuntajeValen(double puntaje) {
        assertEquals(puntaje, conPuntaje(puntaje).puntaje());
    }

    // Sin tope, votos + 50 desbordaba el int en PuntajeConfiable y el puntaje salía negativo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            negativos,             -1
            uno más que el tope,   100000001
            el entero más grande,  2147483647
            """)
    void losVotosVanDeCeroACienMillones(String caso, int votos) {
        rechaza("Los votos tienen que estar entre 0 y 100.000.000", () -> conVotos(votos));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100_000_000})
    void losBordesDeLosVotosValen(int votos) {
        assertEquals(votos, conVotos(votos).votos());
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, 1894, 2032})
    void elAnioVaDelPrimerCineACincoAniosPorDelante(int anio) {
        rechaza("El año tiene que estar entre 1895 y 2031", () -> conAnio(anio));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1895, 2031})
    void ceroEsSinDatoYLosBordesValen(int anio) {
        assertEquals(anio, conAnio(anio).anio());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            director,        101,  0,    0,  0,   El director no puede tener más de 100 caracteres
            sinopsis,        0,    5001, 0,  0,   La sinopsis no puede tener más de 5000 caracteres
            idioma original, 0,    0,    41, 0,   El idioma original no puede tener más de 40 caracteres
            póster,          0,    0,    0,  256, La URL del póster no puede tener más de 255 caracteres
            """)
    void losTextosNoPasanElLargoDeSuColumna(String caso, int director, int sinopsis, int idioma, int poster,
            String mensaje) {
        rechaza(mensaje, () -> VACIO.conCambios("x".repeat(director), "x".repeat(sinopsis), null,
                "x".repeat(idioma), "https://" + "x".repeat(Math.max(0, poster - 8)), null, null, HOY));
    }

    @Test
    void losTextosEntranJustoEnSuColumna() {
        CatalogoPelicula lleno = VACIO.conCambios("x".repeat(100), "x".repeat(5000), null, "x".repeat(40),
                "https://" + "x".repeat(247), null, null, HOY);

        assertEquals(5000, lleno.sinopsis().length());
        assertEquals(255, lleno.posterUrl().length());
    }

    @Test
    void elDirectorYElIdiomaSeGuardanSinLosEspaciosDeLosBordes() {
        CatalogoPelicula catalogo = VACIO.conCambios("  Denis Villeneuve ", null, null, " Inglés ",
                null, null, null, HOY);

        assertEquals("Denis Villeneuve", catalogo.director());
        assertEquals("Inglés", catalogo.idiomaOriginal());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            una ruta suelta,     /duna.jpg
            sin esquema,         image.tmdb.org/duna.jpg
            otro esquema,        javascript:alert(1)
            ftp,                 ftp://imagenes/duna.jpg
            """)
    void elPosterEsUnaUrlHttpOHttps(String caso, String url) {
        rechaza("La URL del póster tiene que empezar con http:// o https://", () -> conPoster(url));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin póster, ''
            http,       http://poster.jpg
            https,      https://image.tmdb.org/t/p/w500/duna.jpg
            """)
    void elPosterVacioOConHttpValen(String caso, String url) {
        assertEquals(url, conPoster(url).posterUrl());
    }

    // El primer error es el que daba el gestor aplicando los cambios uno por uno: el puntaje antes que el año.
    @Test
    void conVariosDatosMalGanaElPuntaje() {
        rechaza("El puntaje tiene que estar entre 0 y 10",
                () -> VACIO.conCambios("x".repeat(101), null, -3, null, "/duna.jpg", 11.0, -1, HOY));
    }
}
