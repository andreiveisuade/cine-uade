package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class TrailerYoutubeTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            id solo,                   dQw4w9WgXcQ,                                         dQw4w9WgXcQ
            id con espacios,           '  dQw4w9WgXcQ  ',                                   dQw4w9WgXcQ
            id con guion y guion bajo, a-b_c-d_e-f,                                         a-b_c-d_e-f
            watch,                     https://www.youtube.com/watch?v=dQw4w9WgXcQ,         dQw4w9WgXcQ
            watch con minuto,          https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s,   dQw4w9WgXcQ
            watch con v al final,      https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ, dQw4w9WgXcQ
            watch del celular,         https://m.youtube.com/watch?v=dQw4w9WgXcQ,           dQw4w9WgXcQ
            sin https,                 www.youtube.com/watch?v=dQw4w9WgXcQ,                 dQw4w9WgXcQ
            corta,                     https://youtu.be/dQw4w9WgXcQ,                        dQw4w9WgXcQ
            corta con parametros,      https://youtu.be/dQw4w9WgXcQ?si=abc,                 dQw4w9WgXcQ
            embed,                     https://www.youtube.com/embed/dQw4w9WgXcQ,           dQw4w9WgXcQ
            """)
    void seQuedaConElIdDelVideo(String caso, String texto, String id) {
        assertEquals(id, new TrailerYoutube(texto).id());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            id corto,              dQw4w9WgXc
            id largo,              dQw4w9WgXcQQ
            id con simbolos,       dQw4w9WgX!Q
            otro sitio,            https://vimeo.com/dQw4w9WgXcQ
            youtube sin video,     https://www.youtube.com/
            youtube falso,         https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ
            watch sin v,           https://www.youtube.com/watch?list=dQw4w9WgXcQ
            id largo en la url,    https://youtu.be/dQw4w9WgXcQQ
            javascript,            javascript:alert(1)
            vacio,                 '   '
            """)
    void loQueNoEsUnVideoDeYoutubeSeRechaza(String caso, String texto) {
        assertEquals("El trailer tiene que ser un video de YouTube",
                assertThrows(DatoInvalido.class, () -> new TrailerYoutube(texto)).getMessage());
    }

    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {"", "no es un video"})
    void siEsNoRechazaLoQueNoEsUnVideo(String texto) {
        assertTrue(TrailerYoutube.siEs(texto).isEmpty());
    }
}
