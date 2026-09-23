package ar.uade.cine.controller.http;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Texto crudo de HTTP a tipos del dominio, con mensajes que lee el usuario: nombran el campo
 * y no dejan asomar clases de Java.
 */
public final class Parseo {

    private Parseo() {
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

    // Los opcionales son para filtros: si no vino, null y no un error. Mal escrito sí falla.

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

    /** Boolean y no boolean: null es "no filtres por esto". */
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
