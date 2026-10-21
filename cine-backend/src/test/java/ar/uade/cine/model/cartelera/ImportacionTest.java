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

    @Test
    void terminarGuardaElResumenYLaHora() {
        Importacion importacion = new Importacion(1, AHORA);
        ResumenDeCorrida resumen = new ResumenDeCorrida(2, 1, 0, "+ [1] Duna\n+ [2] Alien");

        importacion.terminar(resumen, AHORA.plusMinutes(1));

        assertEquals(EstadoImportacion.TERMINADA, importacion.getEstado());
        assertEquals(2, importacion.getNuevas());
        assertEquals(1, importacion.getSalteadas());
        assertEquals("+ [1] Duna\n+ [2] Alien", importacion.getDetalle());
        assertEquals(AHORA.plusMinutes(1), importacion.getTerminoEn());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            terminar una terminada,  true,  true
            fallar una terminada,    true,  false
            terminar una fallida,    false, true
            fallar una fallida,      false, false
            """)
    void unaCorridaCerradaNoCambiaMas(String caso, boolean primeroTermina, boolean despuesTermina) {
        Importacion importacion = new Importacion(1, AHORA);
        cerrar(importacion, primeroTermina);

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> cerrar(importacion, despuesTermina));

        assertEquals("La importación ya terminó", error.getMessage());
    }

    private static void cerrar(Importacion importacion, boolean termina) {
        if (termina) {
            importacion.terminar(new ResumenDeCorrida(0, 0, 0, null), AHORA);
        } else {
            importacion.fallar("TMDB no contesta", AHORA);
        }
    }
}
