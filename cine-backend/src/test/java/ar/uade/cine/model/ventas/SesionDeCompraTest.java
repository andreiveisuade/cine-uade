package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class SesionDeCompraTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin sesión,          ,
            en blanco,           '   '
            """)
    void sinSesionNoSeBloquea(String caso, String sesion) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new SesionDeCompra(sesion));

        assertEquals("Falta la sesión para bloquear butacas", error.getMessage());
    }

    // El largo de la columna: pasado, MySQL rechazaría el INSERT con un 500.
    @Test
    void laSesionTieneElLargoDeLaColumna() {
        assertEquals(64, new SesionDeCompra("x".repeat(64)).valor().length());

        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new SesionDeCompra("x".repeat(65)));

        assertEquals("La sesión no puede tener más de 64 caracteres", error.getMessage());
    }

    @Test
    void seGuardaSinLosEspaciosDeLasPuntasTambienCuandoEsOptativa() {
        assertEquals("sesion-de-ana", new SesionDeCompra("  sesion-de-ana ").valor());
        assertEquals("sesion-de-ana", SesionDeCompra.comoSeGuarda("  sesion-de-ana "));
        assertNull(SesionDeCompra.comoSeGuarda(null));
    }
}
