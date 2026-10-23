package ar.uade.cine.swing.comun;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

// Lee el formato de lo tipeado (números, listas, horas, emails) sin Swing; Validacion lo usa y marca.
// Separado de Validacion para probarlo sin campos ni ventana.
public final class Lecturas {

    private static final Pattern ENTERO = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+([.,]\\d+)?");
    // Letra de fila y número de butaca: A1, J12. Si la butaca existe en la sala lo dice el backend.
    private static final Pattern CODIGO_BUTACA = Pattern.compile("[A-Z]+\\d+");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Lecturas() {
    }

    public static ValorLeido<String> leerTexto(String texto, String nombre, boolean obligatorio) {
        return leer(texto, nombre, obligatorio, ValorLeido::de);
    }

    public static ValorLeido<Integer> leerEntero(String texto, String nombre, boolean obligatorio) {
        return leer(texto, nombre, obligatorio, limpio -> {
            if (!ENTERO.matcher(limpio).matches()) return ValorLeido.falla(noEs(nombre, limpio, "un número entero"));
            try {
                return ValorLeido.de(Integer.valueOf(limpio));
            } catch (NumberFormatException e) {
                return ValorLeido.falla(noEs(nombre, limpio, "un número entero"));
            }
        });
    }

    /** Acepta coma o punto decimal: el encargado tipea "2500,50" tanto como "2500.50". */
    public static ValorLeido<Double> leerDecimal(String texto, String nombre, boolean obligatorio) {
        return leer(texto, nombre, obligatorio, limpio -> DECIMAL.matcher(limpio).matches()
                ? ValorLeido.de(Double.valueOf(limpio.replace(",", ".")))
                : ValorLeido.falla(noEs(nombre, limpio, "un número")));
    }

    /** "8,10,12" → [8, 10, 12]. Un elemento que no es número rechaza la lista entera y se nombra. */
    public static ValorLeido<List<Integer>> leerEnteros(String texto, String nombre, boolean obligatorio) {
        List<String> partes = partes(texto);
        if (partes.isEmpty()) return obligatorio ? ValorLeido.falla(falta(nombre)) : ValorLeido.de(List.of());
        List<Integer> numeros = new ArrayList<>();
        for (String parte : partes) {
            if (parte.isEmpty()) return ValorLeido.falla(sobraComa(nombre));
            ValorLeido<Integer> numero = leerEntero(parte, nombre, true);
            if (!numero.valida()) return ValorLeido.falla(noEs(nombre, parte, "un número entero"));
            numeros.add(numero.valor());
        }
        return ValorLeido.de(numeros);
    }

    /** "a1, B2" → ["A1", "B2"]. Opcional: vacío es la lista vacía. */
    public static ValorLeido<List<String>> leerCodigos(String texto, String nombre) {
        List<String> partes = partes(texto);
        List<String> codigos = new ArrayList<>();
        for (String parte : partes) {
            if (parte.isEmpty()) return ValorLeido.falla(sobraComa(nombre));
            String codigo = parte.toUpperCase(Locale.ROOT);
            if (!CODIGO_BUTACA.matcher(codigo).matches()) {
                return ValorLeido.falla(noEs(nombre, parte, "un código de butaca (fila y número, como A1)"));
            }
            codigos.add(codigo);
        }
        return ValorLeido.de(codigos);
    }

    /** HH:mm, como la pide la API. "9:05" también vale y viaja "09:05". */
    public static ValorLeido<String> leerHora(String texto, String nombre, boolean obligatorio) {
        return leer(texto, nombre, obligatorio, limpio -> {
            try {
                String conCero = limpio.matches("\\d:\\d{2}") ? "0" + limpio : limpio;
                return ValorLeido.de(LocalTime.parse(conCero, HORA).format(HORA));
            } catch (DateTimeParseException e) {
                return ValorLeido.falla(noEs(nombre, limpio, "una hora (HH:mm)"));
            }
        });
    }

    public static ValorLeido<String> leerEmail(String texto, String nombre, boolean obligatorio) {
        return leer(texto, nombre, obligatorio, limpio -> EMAIL.matcher(limpio).matches()
                ? ValorLeido.de(limpio)
                : ValorLeido.falla(noEs(nombre, limpio, "un email")));
    }

    // Lo común a todas: vacío es un faltante si es obligatorio y null si no; lo tipeado, sin espacios, lo interpreta
    // cada una.
    // Lo que abre el botón Ver del trailer: un id suelto es un video de YouTube. Null si no hay nada.
    public static String direccionDelTrailer(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) return null;
        if (!limpio.contains("/")) return "https://www.youtube.com/watch?v=" + limpio;
        return limpio.startsWith("http://") || limpio.startsWith("https://") ? limpio : "https://" + limpio;
    }

    private static <T> ValorLeido<T> leer(String texto, String nombre, boolean obligatorio,
                                          Function<String, ValorLeido<T>> interpretar) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) return obligatorio ? ValorLeido.falla(falta(nombre)) : ValorLeido.de(null);
        return interpretar.apply(limpio);
    }

    static String falta(String nombre) {
        return "Falta completar «" + nombre + "»";
    }

    // Con "8,10," el último vacío también cuenta: split con -1 no lo tira.
    private static List<String> partes(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split(",", -1)).map(String::trim).toList();
    }

    private static String noEs(String nombre, String valor, String que) {
        return "«" + nombre + "»: «" + valor + "» no es " + que;
    }

    private static String sobraComa(String nombre) {
        return "«" + nombre + "»: hay un valor vacío entre comas";
    }
}
