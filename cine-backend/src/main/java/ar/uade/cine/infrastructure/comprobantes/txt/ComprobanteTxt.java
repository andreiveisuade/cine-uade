package ar.uade.cine.infrastructure.comprobantes.txt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;

// Base de los comprobantes .txt (44 columnas, campos, escritura a disco); los generadores la heredan.
abstract class ComprobanteTxt {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINEA = "=".repeat(44);
    // El comprobante lo lee una persona: nunca el nombre de la constante. Es el mismo diccionario que
    // etiquetas.js en la web y Formato en Swing, del lado de quien presenta, no en el modelo.
    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("DOS_D", "2D"), Map.entry("TRES_D", "3D"), Map.entry("IMAX", "IMAX"),
            Map.entry("CUATRO_D", "4D"), Map.entry("DOBLADA", "doblada"), Map.entry("SUBTITULADA", "subtitulada"),
            Map.entry("RESERVADA", "Reservada"), Map.entry("PAGADA", "Pagada"), Map.entry("CANCELADA", "Cancelada"),
            Map.entry("EXPIRADA", "Vencida"), Map.entry("MENOR", "Menor"), Map.entry("JUBILADO", "Jubilado"),
            Map.entry("ESTUDIANTE", "Estudiante"), Map.entry("EFECTIVO", "Efectivo"), Map.entry("DEBITO", "Débito"),
            Map.entry("CREDITO", "Crédito"), Map.entry("QR", "QR"), Map.entry("TRANSFERENCIA", "Transferencia"));

    private final Path directorio;

    protected ComprobanteTxt(Path directorio) {
        this.directorio = directorio;
    }

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

    protected static String etiqueta(Enum<?> valor) {
        return ETIQUETAS.getOrDefault(valor.name(), valor.name());
    }

    protected static String centrar(String texto) {
        int espacios = Math.max((LINEA.length() - texto.length()) / 2, 0);
        return " ".repeat(espacios) + texto;
    }
}
