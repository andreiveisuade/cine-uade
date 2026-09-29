package ar.uade.cine.controller.http;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Pasa los textos del pedido a enums, fechas y números; lo inválido sale como DatoInvalido (400).
// queEs nombra el dato con su artículo («la fecha de inicio»): el mensaje lo usa para concordar.
public final class Parseo {

    private Parseo() {
    }

    public static <T extends Enum<T>> T constante(Class<T> tipo, String valor, String queEs) {
        exigir(valor, queEs);
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // La de Enum.valueOf trae un texto técnico: se traduce a uno que nombra el dato.
            throw new DatoInvalido("Valor inválido para " + queEs + ": " + valor);
        }
    }

    public static <T extends Enum<T>> List<T> constantes(Class<T> tipo, List<String> valores, String queEs) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().map(v -> constante(tipo, v, queEs)).toList();
    }

    public static LocalDateTime momento(String valor, String queEs) {
        return tiempo(valor, queEs, LocalDateTime::parse, "AAAA-MM-DDTHH:MM");
    }

    public static LocalTime hora(String valor, String queEs) {
        return tiempo(valor, queEs, LocalTime::parse, "HH:MM");
    }

    public static LocalDate dia(String valor, String queEs) {
        return tiempo(valor, queEs, LocalDate::parse, "AAAA-MM-DD");
    }

    public static LocalDate diaOpcional(String valor, String queEs) {
        return vacio(valor) ? null : dia(valor, queEs);
    }

    public static LocalTime horaOpcional(String valor, String queEs) {
        return vacio(valor) ? null : hora(valor, queEs);
    }

    public static Integer numeroOpcional(String valor, String queEs) {
        if (vacio(valor)) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new DatoInvalido(conMayuscula(queEs) + " tiene que ser un número");
        }
    }

    public static <T extends Enum<T>> T constanteOpcional(Class<T> tipo, String valor, String queEs) {
        return vacio(valor) ? null : constante(tipo, valor, queEs);
    }

    // Boolean: null es "no filtres por esto".
    public static Boolean booleanOpcional(String valor, String queEs) {
        if (vacio(valor)) {
            return null;
        }
        String limpio = valor.trim().toLowerCase();
        if (limpio.equals("true") || limpio.equals("false")) {
            return Boolean.valueOf(limpio);
        }
        throw new DatoInvalido(conMayuscula(queEs) + " tiene que ser true o false");
    }

    // El formato va en el mensaje: «no es válida» solo, sin decir cómo escribirla, no le sirve a nadie.
    private static <T> T tiempo(String valor, String queEs, Function<String, T> parser, String formato) {
        exigir(valor, queEs);
        try {
            return parser.apply(valor.trim());
        } catch (DateTimeParseException e) {
            String valida = queEs.startsWith("la ") ? "válida" : "válido";
            throw new DatoInvalido(conMayuscula(queEs) + " no es " + valida + ": usá " + formato);
        }
    }

    private static void exigir(String valor, String queEs) {
        if (vacio(valor)) {
            throw new DatoInvalido("Falta " + queEs);
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String conMayuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
