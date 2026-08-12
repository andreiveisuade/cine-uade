package ar.uade.cine.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashea contraseñas para no guardarlas en texto plano: si alguien ve la base, no
 * puede leer las claves.
 *
 * <p>Es SHA-256 sin salt, que alcanza para el TP pero NO para producción: dos usuarios
 * con la misma clave dan el mismo hash, y existen tablas precalculadas para revertirlo.
 * Lo correcto en un sistema real es bcrypt o Argon2.
 */
public class Password {

    private Password() {
    }

    public static String hashear(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 debería estar siempre disponible", e);
        }
    }

    public static boolean coincide(String password, String hashGuardado) {
        return password != null && hashear(password).equals(hashGuardado);
    }
}
