package ar.uade.cine.infrastructure.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * El {@link PasswordEncoder} que Spring Security usa para comparar la clave que viaja en
 * {@code Authorization} contra la guardada, delegando en {@link Password}.
 *
 * <p>Existe para no migrar los hashes. Lo natural con Spring Security sería bcrypt, pero
 * la base desplegada y el seed del encargado ({@code cine-docker/seed/02-admin.sql}) ya
 * tienen SHA-256 hex: cambiar de algoritmo obligaba a regenerar cada clave, y un volumen
 * de MySQL que no se recrea dejaba al encargado afuera. Con esto el login de
 * {@code POST /api/sesion} y el filtro de Spring comparan exactamente igual, y el día que
 * se pase a bcrypt se cambia esta clase sola.
 */
public class PasswordSha256 implements PasswordEncoder {

    @Override
    public String encode(CharSequence password) {
        return Password.hashear(password.toString());
    }

    /**
     * Una clave vacía es un rechazo y no un error: {@code Authorization: Basic} con
     * {@code "email:"} es un pedido mal hecho de alguien, y {@link Password#hashear} lo
     * tomaría como un dato inválido del negocio y saldría como 500.
     */
    @Override
    public boolean matches(CharSequence password, String hashGuardado) {
        if (password == null || password.toString().isBlank()) {
            return false;
        }
        return Password.coincide(password.toString(), hashGuardado);
    }
}
