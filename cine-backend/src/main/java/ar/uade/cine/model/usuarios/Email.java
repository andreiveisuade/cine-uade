package ar.uade.cine.model.usuarios;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Email de un usuario, sin espacios y en minúsculas con forma usuario@dominio.algo; Value Object.
// Normalizado es como se guarda y se busca: la API promete emails sin distinguir mayúsculas.
public record Email(String valor) {

    // VARCHAR(100) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO = 100;

    // Misma regla que Swing; única del backend, porque POST /api/reservas da de alta al cliente sin pasar por el DTO.
    private static final Pattern FORMA = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    // Se mide ya normalizado, o " ana@mail.com" escaparía al chequeo del repetido. El largo va antes que la
    // forma: la regex sobre un texto enorme es lenta (ReDoS) y en Render no hay nginx que corte el cuerpo.
    public Email {
        valor = Regla.texto(valor).obligatorio("Falta el email").recortado().valor().toLowerCase(Locale.ROOT);
        Regla.texto(valor).hasta(LARGO_MAXIMO, "El email");
        if (!FORMA.matcher(valor).matches()) {
            throw new DatoInvalido("El email tiene que tener la forma usuario@dominio.com");
        }
    }

    // R25: el código del ticket es media prueba de dueño; mostrarlo junto al email completo daría las dos mitades.
    public String enmascarado() {
        int arroba = valor.indexOf('@');
        return valor.charAt(0) + "***" + valor.substring(arroba);
    }

    // Para buscar, no guardar: lo que no es un email no encuentra a nadie en vez de dar 400.
    public static Optional<Email> paraBuscar(String texto) {
        try {
            return Optional.of(new Email(texto));
        } catch (DatoInvalido noEsUnEmail) {
            return Optional.empty();
        }
    }
}
