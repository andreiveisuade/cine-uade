package ar.uade.cine.model.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class ImportacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 13, 10, 0);

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            cero,           0
            negativas,      -1
            más de tres,    4
            """)
    void lasPaginasVanDeUnaATres(String caso, int paginas) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new Importacion(paginas, AHORA));

        assertEquals("Las páginas a importar tienen que estar entre 1 y 3", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void losBordesValen(int paginas) {
        assertEquals(paginas, new Importacion(paginas, AHORA).getPaginas());
    }

    @Test
    void sinPaginasSeTraeUna() {
        assertEquals(1, new Importacion(null, AHORA).getPaginas());
    }
}
