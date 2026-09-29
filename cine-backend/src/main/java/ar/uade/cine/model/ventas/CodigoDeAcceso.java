package ar.uade.cine.model.ventas;

import java.security.SecureRandom;
import java.util.Locale;

// Código de acceso de una reserva, la credencial del cliente en la puerta; Value Object que lo genera y normaliza.
// No valida la forma a propósito: un código mal tipeado no es un pedido mal armado sino uno que no
// encuentra reserva, así que sigue saliendo como 404 y no le cuenta a nadie cómo es un código válido.
public record CodigoDeAcceso(String valor) {

    // Sin O, I, 0 ni 1: el código se tipea a mano cuando el escáner no lee.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    // Se busca como se generó: sin espacios y en mayúsculas. Un null es un código vacío, que no
    // encuentra ninguna reserva; antes daba NullPointerException.
    public CodigoDeAcceso {
        valor = valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT);
    }

    public static CodigoDeAcceso generar() {
        StringBuilder codigo = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            codigo.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return new CodigoDeAcceso(codigo.toString());
    }
}
