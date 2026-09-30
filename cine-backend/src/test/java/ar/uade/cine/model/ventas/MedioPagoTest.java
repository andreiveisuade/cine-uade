package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class MedioPagoTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            el efectivo va sin código,             EFECTIVO,      '',        ''
            el efectivo sin código tampoco falla,  EFECTIVO,      ,          ''
            en blanco es sin código,               EFECTIVO,      '   ',     ''
            el electrónico con su código,          QR,            QR-99,     QR-99
            el código se guarda sin espacios,      TRANSFERENCIA, ' TR-1 ',  TR-1
            """)
    void elCodigoSeGuardaComoCorrespondeASuMedio(String caso, MedioPago medio, String codigo, String guardado) {
        assertEquals(guardado, medio.autorizacion(codigo));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            el efectivo no lleva código,        EFECTIVO, AUT-1, El pago en efectivo no lleva código de autorización
            la tarjeta sin código,              CREDITO,  '',    Falta el código de autorización del pago con crédito
            el débito con el código en blanco,  DEBITO,   '  ',  Falta el código de autorización del pago con débito
            el QR sin código,                   QR,       ,      Falta el código de autorización del pago con QR
            """)
    void unCodigoQueNoCorrespondeASuMedioSeRechaza(String caso, MedioPago medio, String codigo,
                                                   String mensaje) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> medio.autorizacion(codigo));

        assertEquals(mensaje, error.getMessage());
    }

    // El VARCHAR(50) de la tabla: pasado, MySQL rechazaría el INSERT con un 500.
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = MedioPago.class, names = "EFECTIVO", mode = EnumSource.Mode.EXCLUDE)
    void elCodigoTieneElLargoDeLaColumna(MedioPago medio) {
        assertEquals("x".repeat(50), medio.autorizacion("x".repeat(50)));

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> medio.autorizacion("x".repeat(51)));

        assertEquals("El código de autorización no puede tener más de 50 caracteres", error.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = MedioPago.class, names = "EFECTIVO", mode = EnumSource.Mode.EXCLUDE)
    void losElectronicosVanPorCheckout(MedioPago medio) {
        assertDoesNotThrow(medio::exigirCheckout);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = MedioPago.class, names = "EFECTIVO")
    void elEfectivoSeCobraEnLaCaja(MedioPago medio) {
        DatoInvalido error = assertThrows(DatoInvalido.class, medio::exigirCheckout);

        assertEquals("El pago con efectivo no va por checkout: se cobra en la caja del cine", error.getMessage());
    }
}
