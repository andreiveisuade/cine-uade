package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// El problema del cambio con películas de 60 y 100 minutos y 15 de limpieza, sin base ni reloj.
class HuecoAlCierreTest {

    private final HuecoAlCierre hueco = new HuecoAlCierre(List.of(60, 100), 15, 600);

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la de 100 deja 35 donde no entra nada,       135, 100, 35, true
            la de 60 deja lugar para otra de 60 justa,  135, 60,  0,  false
            sin lugar para otra después de la limpieza, 110, 60,  50, true
            con tiempo de sobra elige la mejor combinación, 600, 100, 5, false
            """)
    void midElHuecoQueQuedaAlCierre(String caso, int minutosHastaElCierre, int duracion, int esperado,
                                    boolean ultimo) {
        assertEquals(esperado, hueco.siVa(minutosHastaElCierre, duracion), caso);
        assertEquals(ultimo, hueco.esElUltimo(minutosHastaElCierre, duracion), caso);
    }
}
