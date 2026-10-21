package ar.uade.cine.model.imagenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class ImagenUrlTest {

    private static final String SUBIDA = "/api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.png";

    @Test
    void unaSubidaAlSistemaSeAcepta() {
        assertEquals(SUBIDA, new ImagenUrl(SUBIDA).valor());
        assertEquals(SUBIDA, new ImagenUrl("  " + SUBIDA + " ").valor());
    }

    @ParameterizedTest(name = "imagen [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void vaciaEsSinImagen(String url) {
        assertNull(new ImagenUrl(url).valor());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            de otro sitio,        https://otro.com/foto.png
            otra ruta de la api,  /api/candy/productos/1
            sin nombre,           /api/imagenes/
            subiendo de carpeta,  /api/imagenes/../application.yml
            extension ajena,      /api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.gif
            nombre que no es uuid, /api/imagenes/foto.png
            """)
    void loQueNoSubioElSistemaSeRechaza(String caso, String url) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new ImagenUrl(url));
        assertEquals("La imagen tiene que ser una subida al sistema", error.getMessage());
    }

    @Test
    void elNombreDeUnaNuevaTieneLaExtensionDeSuFormato() {
        NombreDeImagen nombre = NombreDeImagen.nuevo(FormatoImagen.WEBP);

        assertTrue(nombre.valor().endsWith(".webp"));
        assertEquals(nombre, NombreDeImagen.leer(nombre.valor()).orElseThrow());
        assertEquals(nombre.url(), new ImagenUrl(nombre.url()).valor());
    }

    @ParameterizedTest(name = "nombre [{0}]")
    @ValueSource(strings = {"..", "../schema.sql", "a/b.png", "0f8fad5b-d9cb-469f-a165-70867728950e.png/..", ""})
    void unNombreQueNoEsDeUnaSubidaNoSeLee(String nombre) {
        assertTrue(NombreDeImagen.leer(nombre).isEmpty());
    }
}
