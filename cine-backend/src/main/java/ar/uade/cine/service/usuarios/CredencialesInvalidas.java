package ar.uade.cine.service.usuarios;

/**
 * Email o contraseña que no corresponden a ningún empleado.
 *
 * <p>No es un {@code IllegalArgumentException} porque el pedido está bien formado: lo que
 * falla es la identidad de quien llama, y eso es 401 y no 400. El mensaje no dice cuál de
 * los dos datos está mal para no confirmarle a nadie qué emails existen.
 */
public class CredencialesInvalidas extends RuntimeException {

    public CredencialesInvalidas() {
        super("Email o contraseña incorrectos");
    }
}
