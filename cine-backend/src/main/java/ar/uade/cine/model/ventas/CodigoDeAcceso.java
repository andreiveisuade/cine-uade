package ar.uade.cine.model.ventas;

import java.security.SecureRandom;
import java.util.Locale;

// Código de acceso de una reserva, credencial del cliente en la puerta; Value Object que genera y normaliza.
// No valida la forma a propósito: un código mal tipeado es una reserva que no se encuentra (404).
public record CodigoDeAcceso(String valor) {

    // Sin O, I, 0 ni 1: se tipea a mano cuando el escáner no lee.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    // Se busca sin espacios y en mayúsculas; un null es un código vacío y no encuentra reserva.
    public CodigoDeAcceso {
        valor = valor == null ? "" : valor.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    public static CodigoDeAcceso generar() {
        StringBuilder codigo = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            codigo.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return new CodigoDeAcceso(codigo.toString());
    }
}
