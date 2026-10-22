package ar.uade.cine.model.usuarios;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ar.uade.cine.model.validacion.Regla;

// Contraseña en claro de un usuario, antes del hash; Value Object con las reglas del alta.
// No se guarda y toString no la muestra.
public record Contrasena(String valor) {

    private static final int MINIMO = 6;

    // bcrypt usa solo los primeros 72 bytes (UTF-8) y BCrypt.hashpw rechaza los que se pasan con un 500.
    private static final int MAXIMO_EN_BYTES = 72;

    // Sin l, o, 0 ni 1: el encargado la dicta o la anota y el cliente la tipea.
    private static final String ALFABETO = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int LARGO_TEMPORAL = 10;
    private static final SecureRandom AZAR = new SecureRandom();

    public Contrasena {
        Regla.texto(valor).obligatorio("Falta la contraseña");
        Regla.numero(valor.length()).entre(MINIMO, Integer.MAX_VALUE,
                "La contraseña tiene que tener al menos " + MINIMO + " caracteres");
        Regla.numero(valor.getBytes(StandardCharsets.UTF_8).length).entre(0, MAXIMO_EN_BYTES,
                "La contraseña tiene que tener como máximo " + MAXIMO_EN_BYTES
                        + " caracteres, o menos si lleva tildes o eñes");
    }

    // R27: la que asigna la boletería; aleatoria y legible, cumple las reglas del alta.
    public static Contrasena temporal() {
        StringBuilder clave = new StringBuilder(LARGO_TEMPORAL);
        for (int i = 0; i < LARGO_TEMPORAL; i++) {
            clave.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return new Contrasena(clave.toString());
    }

    @Override
    public String toString() {
        return "Contrasena[oculta]";
    }
}
