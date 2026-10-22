package ar.uade.cine.infrastructure.seguridad;

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
}
