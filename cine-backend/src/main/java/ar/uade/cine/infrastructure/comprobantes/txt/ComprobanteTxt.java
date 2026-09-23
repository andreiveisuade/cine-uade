package ar.uade.cine.infrastructure.comprobantes.txt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;

/**
 * Lo que comparten los cuatro comprobantes en texto: el ancho, el formato de fecha, cómo
 * se arma una línea y cómo se escribe el archivo. Cada generador solo dice qué líneas
 * lleva el suyo.
 *
 * <p>Antes cada uno tenía su copia de todo esto, y un cambio de formato —el ancho de la
 * línea, el directorio— había que hacerlo cuatro veces.
 */
abstract class ComprobanteTxt {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    protected ComprobanteTxt(Path directorio) {
        this.directorio = directorio;
    }

    /**
     * Escribe el archivo, creando el directorio si hace falta.
     *
     * @param queEs cómo nombrar el comprobante en el error, por ejemplo "el ticket de la
     *              reserva 12"
     */
    protected final void escribir(String nombreArchivo, List<String> lineas, String queEs) {
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombreArchivo), lineas);
        } catch (IOException e) {
            throw new ComprobanteException("No se pudo emitir " + queEs, e);
        }
    }

    protected static String linea() {
        return LINEA;
    }

    protected static String fecha(LocalDateTime momento) {
        return momento.format(FORMATO_FECHA);
    }

    protected static String campo(String etiqueta, String valor) {
        return String.format(" %-13s: %s", etiqueta, valor);
    }

    protected static String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }
}
