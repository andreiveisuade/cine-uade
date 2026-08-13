package ar.uade.cine.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Hablar con quien está del otro lado de la pantalla: leer lo que tipea y mostrarle
 * cosas. Es el <strong>único</strong> lugar del programa donde aparecen Scanner y
 * System.out.
 *
 * <p>Antes eso estaba mezclado con los ocho menús en una sola clase, así que cada uno
 * repetía su propio {@code System.out.print} y su propio parseo. Acá está una vez, y por
 * eso los menús se leen como lo que hacen y no como lo que imprimen.
 */
public class Consola {

    /** El formato con el que se tipean y se muestran las fechas en la consola. */
    public static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Scanner scanner = new Scanner(System.in);

    public void mostrar(String texto) {
        System.out.println(texto);
    }

    /** Sin salto de línea: lo que se usa para dejar el cursor esperando una respuesta. */
    public void pedir(String etiqueta) {
        System.out.print(etiqueta);
    }

    public String leer() {
        return scanner.nextLine().trim();
    }

    public String leerTexto(String etiqueta) {
        pedir(etiqueta);
        return leer();
    }

    public int leerEntero(String etiqueta) {
        pedir(etiqueta);
        return Integer.parseInt(leer());
    }

    public double leerDecimal(String etiqueta) {
        pedir(etiqueta);
        return Double.parseDouble(leer());
    }

    public LocalDateTime leerFecha() {
        pedir("Fecha y hora (dd/MM/yyyy HH:mm): ");
        try {
            return LocalDateTime.parse(leer(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de fecha inválido. Ejemplo: 25/12/2026 20:30");
        }
    }

    /** Muestra las opciones numeradas y devuelve la elegida. Sirve para cualquier enum. */
    @SafeVarargs
    public final <T> T elegir(String que, T... opciones) {
        listarOpciones(opciones);
        return porIndice(opciones, leerEntero(que + ": "), que);
    }

    public <T> void listarOpciones(T[] opciones) {
        for (int i = 0; i < opciones.length; i++) {
            mostrar("  " + (i + 1) + ". " + opciones[i]);
        }
    }

    public <T> T porIndice(T[] opciones, int numero, String que) {
        if (numero < 1 || numero > opciones.length) {
            throw new IllegalArgumentException(que + " inexistente: " + numero);
        }
        return opciones[numero - 1];
    }

    public void imprimir(List<?> elementos) {
        if (elementos.isEmpty()) {
            mostrar("(no hay nada cargado)");
            return;
        }
        elementos.forEach(elemento -> mostrar(String.valueOf(elemento)));
    }
}
