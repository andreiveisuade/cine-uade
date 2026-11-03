package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.salas.Asiento;
import ar.uade.cine.model.salas.TipoAsiento;

class EntradaTest {

    private static final Dinero PRECIO_DE_LA_BUTACA = Dinero.de(5000);

    @Test
    void sinButacaNoHayEntrada() {
        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Entrada(null, TipoTarifa.GENERAL, PRECIO_DE_LA_BUTACA, Proyeccion.DOS_D));

        assertEquals("Falta la butaca de la entrada", error.getMessage());
    }

    // R9: es del asiento, así que no se vende en ninguna función.
    @Test
    void unaButacaFueraDeServicioNoSeVende() {
        Asiento rota = butaca();
        rota.marcarFueraDeServicio();

        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Entrada(rota, TipoTarifa.GENERAL, PRECIO_DE_LA_BUTACA, Proyeccion.DOS_D));

        assertEquals("La butaca A1 está fuera de servicio", error.getMessage());
    }

    @Test
    void sinTarifaEsGeneral() {
        Entrada entrada = new Entrada(butaca(), null, PRECIO_DE_LA_BUTACA, Proyeccion.DOS_D);

        assertEquals(TipoTarifa.GENERAL, entrada.tarifa());
        assertEquals(PRECIO_DE_LA_BUTACA, entrada.precio());
    }

    // Los anteojos van después de la tarifa y enteros: un jubilado paga la mitad de la butaca, no medio anteojo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            la general paga la butaca entera, GENERAL,    DOS_D,  5000
            menor,                            MENOR,      DOS_D,  3000
            jubilado,                         JUBILADO,   DOS_D,  2500
            estudiante,                       ESTUDIANTE, DOS_D,  3500
            la general en 3D suma los anteojos, GENERAL,  TRES_D, 6000
            el jubilado en 3D paga los anteojos enteros, JUBILADO, TRES_D, 3500
            """)
    void laEntradaLeAplicaSuTarifaAlPrecioDeLaButacaYSumaElRecargo3D(String caso, TipoTarifa tarifa,
            Proyeccion proyeccion, double precio) {
        assertEquals(Dinero.de(precio), new Entrada(butaca(), tarifa, PRECIO_DE_LA_BUTACA, proyeccion).precio());
    }

    private static Asiento butaca() {
        return new Asiento(null, 1, 1, TipoAsiento.ESTANDAR);
    }
}
