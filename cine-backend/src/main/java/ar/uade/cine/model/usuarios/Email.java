package ar.uade.cine.model.usuarios;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// El email de un usuario, sin espacios, en minúsculas y con forma usuario@dominio.algo; Value Object.
// Normalizado es como se guarda y por eso como se busca (clientes, reservas por email, login): API.md
// promete emails sin distinguir mayúsculas, y comparando el texto exacto «BETO@x.com» era otro cliente.
public record Email(String valor) {

    // El VARCHAR(100) de la tabla: pasado, MySQL rechaza el INSERT con un 500.
    private static final int LARGO_MAXIMO = 100;

    // La misma regla que Swing. Es la única del backend: POST /api/reservas da de alta al cliente sin
    // pasar por el DTO, y el @Email de Bean Validation aceptaba «a@b».
    private static final Pattern FORMA = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    // Se mide ya normalizado: con los espacios de más, " ana@mail.com" sería otro email y el chequeo
    // del repetido no lo vería.
    public Email {
        valor = Regla.texto(valor).obligatorio("Falta el email").recortado().valor().toLowerCase(Locale.ROOT);
        if (!FORMA.matcher(valor).matches()) {
            throw new DatoInvalido("El email tiene que tener la forma usuario@dominio.com");
        }
        Regla.texto(valor).hasta(LARGO_MAXIMO, "El email");
    }

    // Para buscar, no para guardar: lo que no es un email no encuentra a nadie, y no es un error.
    // Rechazarlo convertiría GET /api/clientes sin email en un 400, y el 401 del login cambiaría de texto.
    public static Optional<Email> paraBuscar(String texto) {
        try {
            return Optional.of(new Email(texto));
        } catch (DatoInvalido noEsUnEmail) {
            return Optional.empty();
        }
    }
}
