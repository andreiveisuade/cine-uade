package ar.uade.cine.model.usuarios;

import java.nio.charset.StandardCharsets;

import ar.uade.cine.model.validacion.Regla;

// La contraseña en claro de un empleado nuevo, antes del hash; Value Object con las reglas del alta.
// No se guarda: el gestor la pasa a bcrypt y la descarta. toString no la muestra, por si llega a un log.
public record Contrasena(String valor) {

    private static final int MINIMO = 6;

    // bcrypt usa solo los primeros 72 bytes, y BCrypt.hashpw rechaza los que se pasan con una
    // IllegalArgumentException en inglés que salía como 500. Se cuentan en UTF-8, como los cuenta bcrypt:
    // una tilde o una eñe ocupan dos.
    private static final int MAXIMO_EN_BYTES = 72;

    public Contrasena {
        Regla.texto(valor).obligatorio("Falta la contraseña");
        Regla.numero(valor.length()).entre(MINIMO, Integer.MAX_VALUE,
                "La contraseña tiene que tener al menos " + MINIMO + " caracteres");
        Regla.numero(valor.getBytes(StandardCharsets.UTF_8).length).entre(0, MAXIMO_EN_BYTES,
                "La contraseña tiene que tener como máximo " + MAXIMO_EN_BYTES
                        + " caracteres, o menos si lleva tildes o eñes");
    }

    @Override
    public String toString() {
        return "Contrasena[oculta]";
    }
}
