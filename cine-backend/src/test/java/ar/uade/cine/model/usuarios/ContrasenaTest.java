package ar.uade.cine.model.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class ContrasenaTest {

    private static final String MUY_LARGA =
            "La contraseña tiene que tener como máximo 72 caracteres, o menos si lleva tildes o eñes";

    private static void rechaza(String mensaje, String clave) {
        assertEquals(mensaje, assertThrows(DatoInvalido.class, () -> new Contrasena(clave)).getMessage());
    }

    // Seis espacios tienen el largo, pero no son una clave.
    @ParameterizedTest(name = "clave [{0}]")
    @NullSource
    @ValueSource(strings = {"", "      "})
    void sinClaveFaltaLaContrasena(String clave) {
        rechaza("Falta la contraseña", clave);
    }

    @Test
    void conMenosDeSeisCaracteresEsCorta() {
        rechaza("La contraseña tiene que tener al menos 6 caracteres", "12345");
    }

    // bcrypt cuenta bytes en UTF-8: una eñe ocupa dos, así que 37 ya no entran.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            73 letras sin tilde,   a, 73
            37 eñes son 74 bytes,  ñ, 37
            """)
    void loQueBcryptNoAdmiteSeRechaza(String caso, String letra, int veces) {
        rechaza(MUY_LARGA, letra.repeat(veces));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            seis caracteres,       a, 6
            72 letras sin tilde,   a, 72
            36 eñes son 72 bytes,  ñ, 36
            """)
    void entraJustoEnLosLimites(String caso, String letra, int veces) {
        assertEquals(letra.repeat(veces), new Contrasena(letra.repeat(veces)).valor());
    }

    @Test
    void noSeMuestraAlImprimirla() {
        assertFalse(new Contrasena("secreta123").toString().contains("secreta123"));
    }
}
