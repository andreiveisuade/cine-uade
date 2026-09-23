package ar.uade.cine.infrastructure.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;

// El formato viejo, SHA-256 sin salt ni prefijo: queda solo para reconocer los hashes que ya
// tienen el seed y las bases creadas antes de bcrypt. Nada nuevo se guarda así.
public class PasswordSha256 implements PasswordEncoder {

    @Override
    public String encode(CharSequence password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 debería estar siempre disponible", e);
        }
    }

    @Override
    public boolean matches(CharSequence password, String hashGuardado) {
        if (password == null || password.toString().isBlank()) {
            return false;
        }
        return encode(password).equals(hashGuardado);
    }
}
