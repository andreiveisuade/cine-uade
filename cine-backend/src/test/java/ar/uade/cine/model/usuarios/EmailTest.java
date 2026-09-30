package ar.uade.cine.model.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class EmailTest {

    private static void rechaza(String mensaje, String email) {
        assertEquals(mensaje, assertThrows(DatoInvalido.class, () -> new Email(email)).getMessage());
    }

    // La regla es la de Swing: con @Email en el DTO, «a@b» pasaba, y POST /api/reservas no pasa por el DTO.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin arroba,          ana.cine.com
            sin dominio,         a@
            dominio sin punto,   a@b
            sin usuario,         @cine.com
            con espacio,         'ana @cine.com'
            dos arrobas,         a@b@cine.com
            """)
    void unEmailSinLaFormaUsuarioArrobaDominioSeRechaza(String caso, String email) {
        rechaza("El email tiene que tener la forma usuario@dominio.com", email);
    }

    @ParameterizedTest(name = "email [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void sinEmailFaltaElEmail(String email) {
        rechaza("Falta el email", email);
    }

    // API.md promete emails sin distinguir mayúsculas: «BETO@x.com» y «beto@x.com» eran dos clientes.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            mayúsculas,              BETO@Cine.com,        beto@cine.com
            espacios en las puntas,  '  ana@cine.com  ',   ana@cine.com
            las dos cosas,           ' Ana@CINE.com.ar ',  ana@cine.com.ar
            """)
    void seGuardaSinEspaciosDeMasYEnMinusculas(String caso, String escrito, String guardado) {
        assertEquals(guardado, new Email(escrito).valor());
    }

    // Con el idioma turco, «I» en minúscula es «ı» sin punto: ese email ya no se encontraría.
    @Test
    void lasMinusculasNoDependenDelIdiomaDelServidor() {
        Locale antes = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            assertEquals("info@cine.com", new Email("INFO@CINE.COM").valor());
        } finally {
            Locale.setDefault(antes);
        }
    }

    // El VARCHAR(100) de la columna, medido ya sin los espacios de las puntas.
    @Test
    void entraHastaCienCaracteres() {
        String cien = "a".repeat(88) + "@cine.com.ar";

        assertEquals(cien, new Email("  " + cien + "  ").valor());
        rechaza("El email no puede tener más de 100 caracteres", "a" + cien);
    }

    // Buscar no es guardar: lo que no es un email no encuentra a nadie, y no es un error.
    @ParameterizedTest(name = "[{0}]")
    @NullSource
    @ValueSource(strings = {"", "   ", "sin-arroba", "a@b"})
    void paraBuscarLoQueNoEsUnEmailNoBuscaANadie(String texto) {
        assertTrue(Email.paraBuscar(texto).isEmpty());
    }

    @Test
    void paraBuscarNormalizaIgualQueAlGuardar() {
        assertEquals(Optional.of(new Email("beto@cine.com")), Email.paraBuscar(" Beto@CINE.com "));
    }
}
