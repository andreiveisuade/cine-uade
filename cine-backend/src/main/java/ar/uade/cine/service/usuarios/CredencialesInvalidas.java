package ar.uade.cine.service.usuarios;

// No es IllegalArgumentException: falla la identidad, y eso es 401 y no 400.
public class CredencialesInvalidas extends RuntimeException {

    public CredencialesInvalidas() {
        super("Email o contraseña incorrectos");
    }
}
