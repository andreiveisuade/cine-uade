package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoAsiento;

class EntradaTest {

    private static final Dinero PRECIO_DE_LA_BUTACA = Dinero.de(5000);

    @Test
    void sinButacaNoHayEntrada() {
        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Entrada(null, TipoTarifa.GENERAL, PRECIO_DE_LA_BUTACA));

        assertEquals("Falta la butaca de la entrada", error.getMessage());
    }

    // R9: es del asiento, así que no se vende en ninguna función.
    @Test
    void unaButacaFueraDeServicioNoSeVende() {
        Asiento rota = butaca();
        rota.marcarFueraDeServicio();

        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Entrada(rota, TipoTarifa.GENERAL, PRECIO_DE_LA_BUTACA));

        assertEquals("La butaca A1 está fuera de servicio", error.getMessage());
    }

    @Test
    void sinTarifaEsGeneral() {
        Entrada entrada = new Entrada(butaca(), null, PRECIO_DE_LA_BUTACA);

        assertEquals(TipoTarifa.GENERAL, entrada.tarifa());
        assertEquals(PRECIO_DE_LA_BUTACA, entrada.precio());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la general paga la butaca entera, GENERAL,    5000
            menor,                            MENOR,      3000
            jubilado,                         JUBILADO,   2500
            estudiante,                       ESTUDIANTE, 3500
            """)
    void laEntradaLeAplicaSuTarifaAlPrecioDeLaButaca(String caso, TipoTarifa tarifa, double precio) {
        assertEquals(Dinero.de(precio), new Entrada(butaca(), tarifa, PRECIO_DE_LA_BUTACA).precio());
    }

    private static Asiento butaca() {
        return new Asiento(null, 1, 1, TipoAsiento.ESTANDAR);
    }
}
