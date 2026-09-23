package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;
import com.toedter.calendar.JDateChooser;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Revisa el <b>formato y la obligatoriedad</b> de un formulario antes de mandarlo, y nada más. Un campo obligatorio
 * vacío, "abc" en un precio o una "x" en "8,x,12" no son reglas del cine: son un pedido mal armado, y mandarlo igual
 * solo conseguía un mensaje engañoso ("El precio es obligatorio" cuando se tipeó "abc") o un dato perdido en silencio.
 * Lo que sí es regla —precio mayor a cero, rangos, fechas que se pisan, R1 a R19— no se mira acá: lo decide el gestor
 * del backend, y su mensaje se muestra tal cual con {@link #mostrarError}.
 *
 * <p>Se usa una por envío: cada lectura devuelve el valor listo para el pedido (o null si falló) y anota qué falló;
 * {@link #ok()} dice si se puede mandar y, si no, marca los campos con el borde de error de FlatLaf, escribe el
 * motivo junto al formulario y deja el foco en el primero. Las lecturas estáticas ({@code leer*}) son la misma
 * lógica sin Swing, para probarla sola.
 */
public final class Validacion {

    /** El valor leído, o por qué no se pudo leer. Con error, {@code valor} es null. */
    public record Lectura<T>(T valor, String error) {

        static <T> Lectura<T> de(T valor) {
            return new Lectura<>(valor, null);
        }

        static <T> Lectura<T> falla(String error) {
            return new Lectura<>(null, error);
        }

        public boolean valida() {
            return error == null;
        }
    }

    private record Falla(JComponent campo, String texto) {
    }

    private record Nombrado(String palabra, JComponent campo) {
    }

    private static final String OUTLINE = "JComponent.outline";
    private static final String ESCUCHA = "validacion.escucha";
    private static final String BORDE_ORIGINAL = "validacion.borde";
    private static final Pattern ENTERO = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+([.,]\\d+)?");
    // Letra de fila y número de butaca: A1, J12. Si la butaca existe en la sala lo dice el backend.
    private static final Pattern CODIGO_BUTACA = Pattern.compile("[A-Z]+\\d+");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final JLabel mensaje;
    private final List<Falla> fallas = new ArrayList<>();
    private final List<Nombrado> nombrados = new ArrayList<>();

    /** {@code mensaje}: la etiqueta junto al formulario donde va el motivo, propio o del backend. */
    public Validacion(JLabel mensaje) {
        this.mensaje = mensaje;
        mensaje.setForeground(Colores.error());
    }

    // --- lecturas sin Swing ---

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

    // Con "8,10," el último vacío también cuenta: split con -1 no lo tira.
    private static List<String> partes(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split(",", -1)).map(String::trim).toList();
    }

    static String falta(String nombre) {
        return "Falta completar «" + nombre + "».";
    }

    private static String noEs(String nombre, String valor, String que) {
        return "«" + nombre + "»: «" + valor + "» no es " + que + ".";
    }

    private static String sobraComa(String nombre) {
        return "«" + nombre + "»: hay un valor vacío entre comas.";
    }

    // --- lecturas de campos ---

    public String texto(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerTexto(campo.getText(), nombre, obligatorio));
    }

    public Integer entero(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerEntero(campo.getText(), nombre, obligatorio));
    }

    public Double decimal(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerDecimal(campo.getText(), nombre, obligatorio));
    }

    public List<Integer> enteros(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerEnteros(campo.getText(), nombre, obligatorio));
    }

    public List<String> codigos(JTextComponent campo, String nombre) {
        return anotar(campo, nombre, leerCodigos(campo.getText(), nombre));
    }

    public String hora(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerHora(campo.getText(), nombre, obligatorio));
    }

    public String email(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, leerEmail(campo.getText(), nombre, obligatorio));
    }

    /** La fecha en ISO, o null. Vacío en un campo obligatorio cuenta como faltante. */
    public String fecha(JDateChooser selector, String nombre, boolean obligatorio) {
        LocalDate fecha = Fechas.leer(selector);
        JComponent campo = editor(selector);
        if (fecha == null && obligatorio) return anotar(campo, nombre, Lectura.falla(falta(nombre)));
        return anotar(campo, nombre, Lectura.de(fecha == null ? null : fecha.toString()));
    }

    /** Un combo obligatorio: vacío es que el catálogo no llegó o no hay nada cargado (sin salas, sin películas). */
    public <T> T elegido(JComboBox<Opcion<T>> combo, String nombre) {
        T valor = Campos.elegido(combo);
        return anotar(combo, nombre, valor == null ? Lectura.falla(falta(nombre)) : Lectura.de(valor));
    }

    /**
     * Para lo que no es un texto, como un grupo de casillas: {@code completo} dice si se llenó. Es obligatoriedad
     * (que se haya elegido algo), no una regla sobre qué se eligió.
     */
    public void exigir(boolean completo, JComponent campo, String nombre) {
        anotar(campo, nombre, completo ? Lectura.de(Boolean.TRUE) : Lectura.falla(falta(nombre)));
    }

    /** Para que un error del backend que nombra algo con otra palabra ("la vigencia") marque ese campo. */
    public void alMencionar(String palabra, JComponent campo) {
        nombrados.add(new Nombrado(palabra.toLowerCase(Locale.ROOT), campo));
    }

    public void alMencionar(String palabra, JDateChooser selector) {
        alMencionar(palabra, editor(selector));
    }

    private <T> T anotar(JComponent campo, String nombre, Lectura<T> lectura) {
        desmarcar(campo);
        nombrados.add(new Nombrado(primeraPalabra(nombre), campo));
        if (!lectura.valida()) fallas.add(new Falla(campo, lectura.error()));
        return lectura.valor();
    }

    // "Precio base *" → "precio": es la palabra que usan los mensajes del backend ("El precio debe ser mayor a cero").
    private static String primeraPalabra(String nombre) {
        String limpio = nombre.replace("*", "").trim().toLowerCase(Locale.ROOT);
        int espacio = limpio.indexOf(' ');
        return espacio < 0 ? limpio : limpio.substring(0, espacio);
    }

    /**
     * Si todo se pudo leer, borra el mensaje y devuelve true. Si no, marca cada campo con error, escribe los motivos
     * y pone el foco en el primero: el pedido no se manda.
     */
    public boolean ok() {
        if (fallas.isEmpty()) {
            mensaje.setText(" ");
            return true;
        }
        fallas.forEach(f -> marcar(f.campo()));
        mostrar(fallas.stream().map(Falla::texto).distinct().collect(Collectors.toList()));
        fallas.get(0).campo().requestFocusInWindow();
        return false;
    }

    public List<String> errores() {
        return fallas.stream().map(Falla::texto).toList();
    }

    /**
     * El rechazo del backend, tal cual llegó. Si nombra uno de los campos leídos, se marca: "El precio debe ser mayor
     * a cero" marca el precio. Es solo una ayuda para encontrarlo; el texto es el del backend. Solo un rechazo de lo
     * que se mandó es del formulario: sin conexión, un 500 o un 409 no se arreglan tocando un campo y van al diálogo de
     * errores globales.
     */
    public void mostrarError(ErrorApi error) {
        if (error.esSesionVencida()) return;
        if (!error.esDelFormulario()) {
            Mensajes.error(mensaje, error);
            return;
        }
        mostrar(List.of(error.getMessage()));
        String texto = error.getMessage().toLowerCase(Locale.ROOT);
        nombrados.stream()
                .filter(n -> !n.palabra().isEmpty()
                        && Pattern.compile("\\b" + Pattern.quote(n.palabra()) + "\\b",
                        Pattern.UNICODE_CHARACTER_CLASS).matcher(texto).find())
                .findFirst()
                .ifPresent(n -> marcar(n.campo()));
    }

    private void mostrar(List<String> textos) {
        // Sin ancho fijo: la etiqueta es de Componentes.texto y corta línea al ancho del formulario.
        mensaje.setText("<html>" + textos.stream().map(Formato::escapar).collect(Collectors.joining("<br>"))
                + "</html>");
    }

    // --- marcas ---

    private static JComponent editor(JDateChooser selector) {
        return (JComponent) selector.getDateEditor().getUiComponent();
    }

    /** Un texto, un combo o un spinner llevan el borde rojo de FlatLaf; un panel de casillas, una línea roja. */
    static void marcar(JComponent campo) {
        if (admiteOutline(campo)) {
            campo.putClientProperty(OUTLINE, "error");
        } else if (campo.getClientProperty(BORDE_ORIGINAL) == null) {
            Border original = campo.getBorder();
            campo.putClientProperty(BORDE_ORIGINAL, original == null ? BorderFactory.createEmptyBorder() : original);
            campo.setBorder(BorderFactory.createLineBorder(Colores.error()));
        }
        escucharCambios(campo);
    }

    static void desmarcar(JComponent campo) {
        if (admiteOutline(campo)) {
            campo.putClientProperty(OUTLINE, null);
            return;
        }
        Object original = campo.getClientProperty(BORDE_ORIGINAL);
        if (original != null) {
            campo.setBorder((Border) original);
            campo.putClientProperty(BORDE_ORIGINAL, null);
        }
    }

    static boolean marcado(JComponent campo) {
        return "error".equals(campo.getClientProperty(OUTLINE)) || campo.getClientProperty(BORDE_ORIGINAL) != null;
    }

    private static boolean admiteOutline(JComponent campo) {
        return campo instanceof JTextComponent || campo instanceof JComboBox || campo instanceof JSpinner;
    }

    // La marca se va apenas se corrige el campo, sin esperar al próximo envío. Se registra una sola vez por campo.
    private static void escucharCambios(JComponent campo) {
        if (campo.getClientProperty(ESCUCHA) != null) return;
        campo.putClientProperty(ESCUCHA, Boolean.TRUE);
        if (campo instanceof JTextComponent texto) {
            texto.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    desmarcar(campo);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    desmarcar(campo);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    desmarcar(campo);
                }
            });
        } else if (campo instanceof JComboBox<?> combo) {
            combo.addActionListener(e -> desmarcar(campo));
        } else {
            // Un panel de casillas: se desmarca al tildar cualquiera.
            for (Component hijo : campo.getComponents()) {
                if (hijo instanceof AbstractButton casilla) casilla.addItemListener(e -> desmarcar(campo));
            }
        }
    }
}
