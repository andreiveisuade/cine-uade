package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CodigoDeAccesoTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            tal como se generó,         ABCD2345,       ABCD2345
            tipeado en minúsculas,      abcd2345,       ABCD2345
            con espacios en las puntas, '  abcd2345  ', ABCD2345
            en blanco,                  '   ',          ''
            sin código,                 ,               ''
            """)
    void seBuscaComoSeGenero(String caso, String tipeado, String buscado) {
        assertEquals(buscado, new CodigoDeAcceso(tipeado).valor());
    }

    @Test
    void elGeneradoTieneOchoCaracteresQueNoSeConfundenAlTipearlos() {
        for (int i = 0; i < 200; i++) {
            String codigo = CodigoDeAcceso.generar().valor();

            assertEquals(8, codigo.length());
            assertTrue(codigo.matches("[A-HJ-NP-Z2-9]{8}"), codigo);
        }
    }
}
