package ar.uade.cine.infrastructure.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordBcryptTest {

    private final PasswordEncoder claves = new ConfiguracionSeguridad().passwordEncoder();

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            seis letras,           a, 6
            72 letras sin tilde,   a, 72
            36 eñes son 72 bytes,  ñ, 36
            """)
    void loQueEntraEnBcryptSaleEnBcrypt(String caso, String letra, int veces) {
        String clave = letra.repeat(veces);
        String hash = claves.encode(clave);

        assertTrue(hash.startsWith("{bcrypt}"), hash);
        assertTrue(claves.matches(clave, hash));
    }

    // Una clave así solo puede venir de un hash SHA-256 viejo: codificarla da el mismo que ya estaba.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            73 letras sin tilde,   a, 73
            37 eñes son 74 bytes,  ñ, 37
            """)
    void loQueNoEntraQuedaEnElSha256Viejo(String caso, String letra, int veces) {
        String clave = letra.repeat(veces);

        assertEquals(new PasswordSha256().encode(clave), claves.encode(clave));
        assertTrue(claves.matches(clave, claves.encode(clave)));
    }
}
