package ar.uade.cine.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import io.javalin.http.Context;

/**
 * Lectura de lo que entra por HTTP: texto crudo a los tipos del dominio. Los mensajes
 * son los que va a leer el usuario, así que no dejan asomar nombres de clases de Java.
 */
class Parseo {

    static int id(Context ctx) {
        return id(ctx, "id");
    }

    static int id(Context ctx, String nombre) {
        String valor = ctx.pathParam(nombre);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new NoEncontrado("El identificador " + valor + " no es válido");
        }
    }

    /** Los enums viajan con el nombre de la constante, nunca con la etiqueta de mostrar. */
    static <T extends Enum<T>> T constante(Class<T> tipo, String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor inválido para " + queEs + ": " + valor);
        }
    }

    static <T extends Enum<T>> List<T> constantes(Class<T> tipo, List<String> valores, String queEs) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream().map(v -> constante(tipo, v, queEs)).toList();
    }

    static LocalDateTime momento(String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser una fecha y hora válida");
        }
    }

    static LocalDate dia(String valor, String queEs) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + queEs);
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(queEs + " tiene que ser una fecha válida");
        }
    }
}
