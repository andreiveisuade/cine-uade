package ar.uade.cine.api.http;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import io.javalin.http.Context;

/**
 * Lectura de lo que entra por HTTP: texto crudo a los tipos del dominio. Los mensajes
 * son los que va a leer el usuario, así que no dejan asomar nombres de clases de Java.
 */
public final class Parseo {

    private Parseo() {
    }

    public static int id(Context ctx) {
        return id(ctx, "id");
    }

    public static int id(Context ctx, String nombre) {
        String valor = ctx.pathParam(nombre);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new NoEncontrado("El identificador " + valor + " no es válido");
        }
    }

    /** Los enums viajan con el nombre de la constante, nunca con la etiqueta de mostrar. */
    public static <T extends Enum<T>> T constante(Class<T> tipo, String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para " + queEs + ": " + valor);
        }
    }

    public static <T extends Enum<T>> List<T> constantes(Class<T> tipo, List<String> valores, String queEs) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().map(v -> constante(tipo, v, queEs)).toList();
    }

    public static LocalDateTime momento(String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser una fecha y hora válida");
        }
    }

    public static LocalTime hora(String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return LocalTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser una hora válida");
        }
    }

    public static LocalDate dia(String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser una fecha válida");
        }
    }

    /* ------------------------------------------------------- filtros opcionales */
    /*
     * Los tres de abajo son para query params de búsqueda, donde "no vino" y "no filtres
     * por eso" son lo mismo. Por eso devuelven null en vez de fallar, al revés que los de
     * arriba: un campo que falta en un alta es un error del que manda, pero un filtro que
     * falta es lo normal.
     */

    /** Una fecha de filtro, o null si no vino. Si vino mal escrita sí falla. */
    public static LocalDate diaOpcional(String valor, String queEs) {
        return vacio(valor) ? null : dia(valor, queEs);
    }

    public static Integer numeroOpcional(String valor, String queEs) {
        if (vacio(valor)) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser un número");
        }
    }

    public static <T extends Enum<T>> T constanteOpcional(Class<T> tipo, String valor, String queEs) {
        return vacio(valor) ? null : constante(tipo, valor, queEs);
    }

    /**
     * Un filtro de sí/no con tres estados: true, false y "no filtres por esto". Por eso
     * devuelve Boolean y no boolean — la diferencia entre "solo las despublicadas" y
     * "todas" se perdería con un primitivo.
     */
    public static Boolean booleanOpcional(String valor, String queEs) {
        if (vacio(valor)) {
            return null;
        }
        String limpio = valor.trim().toLowerCase();
        if (limpio.equals("true") || limpio.equals("false")) {
            return Boolean.valueOf(limpio);
        }
        throw new IllegalArgumentException(queEs + " tiene que ser true o false");
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
