package ar.uade.cine.controller.http;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import ar.uade.cine.model.rechazos.DatoInvalido;

// Pasa los textos del pedido a enums, fechas y números; lo inválido sale como DatoInvalido (400).
// queEs nombra el dato con su artículo («la fecha de inicio»): el mensaje lo usa para concordar.
public final class Parseo {

    private static final int PRIMER_ANIO = 1000;
    private static final int ULTIMO_ANIO = 9999;

    private Parseo() {
    }

    public static <T extends Enum<T>> T constante(Class<T> tipo, String valor, String queEs) {
        exigir(valor, queEs);
        try {
            // Locale.ROOT: con el de la JVM en turco, "i" pasaría a "İ" y ninguna constante coincidiría.
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // La de Enum.valueOf trae un texto técnico: se traduce a uno que nombra el dato.
            throw new DatoInvalido(noEsValido(queEs) + ": " + valor);
        }
    }

    public static <T extends Enum<T>> List<T> constantes(Class<T> tipo, List<String> valores, String queEs) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().map(v -> constante(tipo, v, queEs)).toList();
    }

    public static LocalDateTime momento(String valor, String queEs) {
        LocalDateTime momento = tiempo(valor, queEs, LocalDateTime::parse, "AAAA-MM-DDTHH:MM");
        exigirAnio(momento.getYear(), queEs);
        return momento;
    }

    public static LocalTime hora(String valor, String queEs) {
        return tiempo(valor, queEs, LocalTime::parse, "HH:MM");
    }

    public static LocalDate dia(String valor, String queEs) {
        LocalDate dia = tiempo(valor, queEs, LocalDate::parse, "AAAA-MM-DD");
        exigirAnio(dia.getYear(), queEs);
        return dia;
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
        String limpio = valor.trim();
        try {
            return Integer.valueOf(limpio);
        } catch (NumberFormatException e) {
            // 99999999999 es un número: lo que no cumple es que entre en un int.
            if (limpio.matches("[+-]?\\d+")) {
                throw new DatoInvalido(conMayuscula(queEs) + " tiene que estar entre "
                        + Integer.MIN_VALUE + " y " + Integer.MAX_VALUE);
            }
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
        String limpio = valor.trim().toLowerCase(Locale.ROOT);
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
            throw new DatoInvalido(noEsValido(queEs) + ": usá " + formato);
        }
    }

    // LocalDate acepta hasta el año +999999999, y con uno así la aritmética de fechas de los gestores
    // tira DateTimeException. El límite es el de DATE y DATETIME en MySQL: técnico, como el largo de
    // un VARCHAR, y por eso se controla acá y no en una regla del cine.
    private static void exigirAnio(int anio, String queEs) {
        if (anio < PRIMER_ANIO || anio > ULTIMO_ANIO) {
            throw new DatoInvalido(conMayuscula(queEs) + " tiene que estar entre los años "
                    + PRIMER_ANIO + " y " + ULTIMO_ANIO);
        }
    }

    // El adjetivo concuerda con el dato: «la proyección no es válida», «el idioma no es válido».
    private static String noEsValido(String queEs) {
        return conMayuscula(queEs) + " no es " + (queEs.startsWith("la ") ? "válida" : "válido");
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
