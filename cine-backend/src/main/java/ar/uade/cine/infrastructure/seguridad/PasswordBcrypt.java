package ar.uade.cine.infrastructure.seguridad;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

// Codifica en bcrypt la clave que entra en sus 72 bytes, y la que no en el SHA-256 viejo; Decorator.
// Lo necesita el re-hash del primer login, que codifica la clave en claro antes de guardarla: con más de
// 72 bytes, BCrypt.hashpw tiraba una IllegalArgumentException adentro del filtro y el login era un 500.
// Una clave así solo puede estar guardada en SHA-256 (el alta pide Contrasena, que no la deja pasar), así
// que lo que sale es el mismo hash que ya estaba: el empleado entra y no se migra nada.
@RequiredArgsConstructor
class PasswordBcrypt implements PasswordEncoder {

    private static final int MAXIMO_DE_BCRYPT = 72;

    private final PasswordEncoder claves;
    private final PasswordEncoder sha256 = new PasswordSha256();

    @Override
    public String encode(CharSequence clave) {
        return entraEnBcrypt(clave) ? claves.encode(clave) : sha256.encode(clave);
    }

    @Override
    public boolean matches(CharSequence clave, String hash) {
        return claves.matches(clave, hash);
    }

    @Override
    public boolean upgradeEncoding(String hash) {
        return claves.upgradeEncoding(hash);
    }

    // En UTF-8, como los cuenta bcrypt: una tilde o una eñe ocupan dos.
    private static boolean entraEnBcrypt(CharSequence clave) {
        return clave.toString().getBytes(StandardCharsets.UTF_8).length <= MAXIMO_DE_BCRYPT;
    }
}
