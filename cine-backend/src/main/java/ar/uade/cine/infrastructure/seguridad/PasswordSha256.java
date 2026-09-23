package ar.uade.cine.infrastructure.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;

// Delega en Password para no migrar los hashes SHA-256 que ya tienen la base y el seed.
public class PasswordSha256 implements PasswordEncoder {

    @Override
    public String encode(CharSequence password) {
        return Password.hashear(password.toString());
    }

    // Clave vacía es rechazo, no error: Password.hashear lanzaría y saldría 500.
    @Override
    public boolean matches(CharSequence password, String hashGuardado) {
        if (password == null || password.toString().isBlank()) {
            return false;
        }
        return Password.coincide(password.toString(), hashGuardado);
    }
}
