package ar.uade.cine.swing.comun;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// Lee el formato de lo que se tipeó (números, listas, horas, emails) sin Swing; Validacion lo usa y marca los campos.
/**
 * Un obligatorio vacío, "abc" en un precio o una "x" en "8,x,12" no son reglas del cine: son un pedido mal armado. Lo
 * que sí es regla (precio mayor a cero, rangos, R1 a R20) no se mira acá, lo decide el backend. Separado de
 * {@link Validacion} para probarlo solo, sin campos ni ventana.
 */
public final class Lecturas {

    private static final Pattern ENTERO = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+([.,]\\d+)?");
    // Letra de fila y número de butaca: A1, J12. Si la butaca existe en la sala lo dice el backend.
    private static final Pattern CODIGO_BUTACA = Pattern.compile("[A-Z]+\\d+");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Lecturas() {
    }

    public static Lectura<String> leerTexto(String texto, String nombre, boolean obligatorio) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(null);
        return Lectura.de(limpio);
    }

    public static Lectura<Integer> leerEntero(String texto, String nombre, boolean obligatorio) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(null);
        if (!ENTERO.matcher(limpio).matches()) return Lectura.falla(noEs(nombre, limpio, "un número entero"));
        try {
            return Lectura.de(Integer.valueOf(limpio));
        } catch (NumberFormatException e) {
            return Lectura.falla(noEs(nombre, limpio, "un número entero"));
        }
    }

    /** Acepta coma o punto decimal: el encargado tipea "2500,50" tanto como "2500.50". */
    public static Lectura<Double> leerDecimal(String texto, String nombre, boolean obligatorio) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(null);
        if (!DECIMAL.matcher(limpio).matches()) return Lectura.falla(noEs(nombre, limpio, "un número"));
        return Lectura.de(Double.valueOf(limpio.replace(",", ".")));
    }

    /** "8,10,12" → [8, 10, 12]. Un elemento que no es número rechaza la lista entera y se nombra. */
    public static Lectura<List<Integer>> leerEnteros(String texto, String nombre, boolean obligatorio) {
        List<String> partes = partes(texto);
        if (partes.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(List.of());
        List<Integer> numeros = new ArrayList<>();
        for (String parte : partes) {
            if (parte.isEmpty()) return Lectura.falla(sobraComa(nombre));
            Lectura<Integer> numero = leerEntero(parte, nombre, true);
            if (!numero.valida()) return Lectura.falla(noEs(nombre, parte, "un número entero"));
            numeros.add(numero.valor());
        }
        return Lectura.de(numeros);
    }

    /** "a1, B2" → ["A1", "B2"]. Opcional: vacío es la lista vacía. */
    public static Lectura<List<String>> leerCodigos(String texto, String nombre) {
        List<String> partes = partes(texto);
        List<String> codigos = new ArrayList<>();
        for (String parte : partes) {
            if (parte.isEmpty()) return Lectura.falla(sobraComa(nombre));
            String codigo = parte.toUpperCase(Locale.ROOT);
            if (!CODIGO_BUTACA.matcher(codigo).matches()) {
                return Lectura.falla(noEs(nombre, parte, "un código de butaca (fila y número, como A1)"));
            }
            codigos.add(codigo);
        }
        return Lectura.de(codigos);
    }

    /** HH:mm, como la pide la API. "9:05" también vale y viaja "09:05". */
    public static Lectura<String> leerHora(String texto, String nombre, boolean obligatorio) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(null);
        try {
            String conCero = limpio.matches("\\d:\\d{2}") ? "0" + limpio : limpio;
            return Lectura.de(LocalTime.parse(conCero, HORA).format(HORA));
        } catch (DateTimeParseException e) {
            return Lectura.falla(noEs(nombre, limpio, "una hora (HH:mm)"));
        }
    }

    public static Lectura<String> leerEmail(String texto, String nombre, boolean obligatorio) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? Lectura.falla(falta(nombre)) : Lectura.de(null);
        if (!EMAIL.matcher(limpio).matches()) return Lectura.falla(noEs(nombre, limpio, "un email"));
        return Lectura.de(limpio);
    }

    static String falta(String nombre) {
        return "Falta completar «" + nombre + "».";
    }

    // Con "8,10," el último vacío también cuenta: split con -1 no lo tira.
    private static List<String> partes(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split(",", -1)).map(String::trim).toList();
    }

    private static String noEs(String nombre, String valor, String que) {
        return "«" + nombre + "»: «" + valor + "» no es " + que + ".";
    }

    private static String sobraComa(String nombre) {
        return "«" + nombre + "»: hay un valor vacío entre comas.";
    }
}
