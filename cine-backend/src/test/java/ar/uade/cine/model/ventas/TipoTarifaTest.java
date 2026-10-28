package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;

class TipoTarifaTest {

    // R16: la reducida ya es un descuento, no suma promoción; solo la general pide acreditación en la puerta.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la general paga entero y participa,  GENERAL,    5000, false, true
            el menor paga el 60 %,               MENOR,      3000, true,  false
            el jubilado paga la mitad,           JUBILADO,   2500, true,  false
            el estudiante paga el 70 %,          ESTUDIANTE, 3500, true,  false
            """)
    @DisplayName("R16: cada tarifa dice cuánto cobra, si se acredita y si suma promociones")
    void cadaTarifaDiceCuantoCobraSiSeAcreditaYSiSumaPromociones(String caso, TipoTarifa tarifa, double precio,
            boolean requiereAcreditacion, boolean participaDePromociones) {
        assertEquals(Dinero.de(precio), tarifa.aplicarA(Dinero.de(5000)));
        assertEquals(requiereAcreditacion, tarifa.requiereAcreditacion());
        assertEquals(participaDePromociones, tarifa.participaDePromociones());
    }

    @Test
    void lasExcluidasDePromocionesSonLasReducidas() {
        assertEquals(List.of(TipoTarifa.MENOR, TipoTarifa.JUBILADO, TipoTarifa.ESTUDIANTE),
                TipoTarifa.excluidasDePromociones());
    }
}
