package ar.uade.cine.infrastructure.comprobantes.txt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;

// Arma un comprobante .txt de 44 columnas y lo escribe a disco; cada Generador*Txt completa lo suyo.
// Patrón Template Method (GoF): imprimir es final, así ningún generador olvida la línea final.
abstract class ComprobanteTxt<T> {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);

    private final Path directorio;

    protected ComprobanteTxt(Path directorio) {
        this.directorio = directorio;
    }

    // El método plantilla: encabezado, cuerpo, leyendas entre líneas y a disco, siempre en ese orden.
    protected final void imprimir(T dato) {
        List<String> lineas = new ArrayList<>(List.of(LINEA, centrar(marca()), centrar(titulo(dato)), LINEA));
        lineas.addAll(cuerpo(dato));
        lineas.add(LINEA);
        leyendas().forEach(leyenda -> lineas.add(centrar(leyenda)));
        lineas.add(LINEA);
        escribir(archivo(dato), lineas, queEs(dato));
    }

    // Gancho con valor por defecto: solo el candy firma distinto.
    protected String marca() {
        return "CINE UADE";
    }

    protected abstract String titulo(T dato);

    protected abstract List<String> cuerpo(T dato);

    protected abstract List<String> leyendas();

    protected abstract String archivo(T dato);

    // Para el mensaje si el archivo no se puede escribir: "el ticket de la reserva 7".
    protected abstract String queEs(T dato);

    private void escribir(String nombreArchivo, List<String> lineas, String queEs) {
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

    private static String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }
}
