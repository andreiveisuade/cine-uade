package ar.uade.cine.model.imagenes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class ImagenTest {

    private static final int DOS_MB = 2 * 1024 * 1024;

    private static byte[] conCabecera(String hex, int largo) {
        byte[] cabecera = HexFormat.of().parseHex(hex);
        return Arrays.copyOf(cabecera, Math.max(largo, cabecera.length));
    }

    private static String rechazo(byte[] contenido) {
        return assertThrows(DatoInvalido.class, () -> Imagen.subida(contenido)).getMessage();
    }

    // El formato sale de los primeros bytes: la extensión del archivo la elige quien sube.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            jpg,  ffd8ffe0,                 JPG
            png,  89504e470d0a1a0a,         PNG
            webp, 52494646000000005745425050, WEBP
            """)
    void reconoceElFormatoPorSuContenido(String caso, String cabecera, FormatoImagen formato) {
        assertEquals(formato, Imagen.subida(conCabecera(cabecera, 64)).formato());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            texto con nombre de png, 686f6c61
            gif,                     47494638396101
            riff que no es webp,     524946460000000057415645
            png cortado,             89504e47
            """)
    void loQueNoEsJpgPngNiWebpSeRechaza(String caso, String cabecera) {
        assertEquals("La imagen tiene que ser JPG, PNG o WEBP", rechazo(HexFormat.of().parseHex(cabecera)));
    }

    @Test
    void dosMegasJustosEntranYUnByteMasNo() {
        assertEquals(DOS_MB, Imagen.subida(conCabecera("ffd8ff", DOS_MB)).contenido().length);
        assertEquals("La imagen tiene que pesar 2 MB como máximo", rechazo(conCabecera("ffd8ff", DOS_MB + 1)));
    }

    @Test
    void sinContenidoFaltaLaImagen() {
        assertEquals("Falta la imagen", rechazo(null));
        assertEquals("Falta la imagen", rechazo(new byte[0]));
    }
}
