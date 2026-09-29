package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;
import com.toedter.calendar.JDateChooser;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Revisa formato y obligatoriedad de un formulario antes de mandarlo; el rechazo del backend, en línea.
/**
 * Revisa el <b>formato y la obligatoriedad</b> de un formulario antes de mandarlo, y nada más. Un campo obligatorio
 * vacío, "abc" en un precio o una "x" en "8,x,12" no son reglas del cine: son un pedido mal armado, y mandarlo igual
 * solo conseguía un mensaje engañoso ("El precio es obligatorio" cuando se tipeó "abc") o un dato perdido en silencio.
 * Lo que sí es regla —precio mayor a cero, rangos, fechas que se pisan, R1 a R20— no se mira acá: lo decide el gestor
 * del backend, y su mensaje se muestra tal cual con {@link #mostrarError}.
 *
 * <p>Se usa una por envío: cada lectura devuelve el valor listo para el pedido (o null si falló) y anota qué falló;
 * {@link #ok()} dice si se puede mandar y, si no, marca los campos ({@link Marcas}), escribe el motivo junto al
 * formulario y deja el foco en el primero. Cómo se lee cada formato está en {@link Lecturas}, sin Swing.
 */
public final class Validacion {

    private record Falla(JComponent campo, String texto) {
    }

    private record Nombrado(String palabra, JComponent campo) {
    }

    private final JLabel mensaje;
    private final List<Falla> fallas = new ArrayList<>();
    private final List<Nombrado> nombrados = new ArrayList<>();

    /** {@code mensaje}: la etiqueta junto al formulario donde va el motivo, propio o del backend. */
    public Validacion(JLabel mensaje) {
        this.mensaje = mensaje;
        mensaje.setForeground(Colores.error());
    }

    // --- lecturas de campos ---

    public String texto(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerTexto(campo.getText(), nombre, obligatorio));
    }

    public Integer entero(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerEntero(campo.getText(), nombre, obligatorio));
    }

    public Double decimal(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerDecimal(campo.getText(), nombre, obligatorio));
    }

    public List<Integer> enteros(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerEnteros(campo.getText(), nombre, obligatorio));
    }

    public List<String> codigos(JTextComponent campo, String nombre) {
        return anotar(campo, nombre, Lecturas.leerCodigos(campo.getText(), nombre));
    }

    public String hora(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerHora(campo.getText(), nombre, obligatorio));
    }

    public String email(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerEmail(campo.getText(), nombre, obligatorio));
    }

    /** La fecha en ISO, o null. Vacío en un campo obligatorio cuenta como faltante. */
    public String fecha(JDateChooser selector, String nombre, boolean obligatorio) {
        LocalDate fecha = Fechas.leer(selector);
        JComponent campo = editor(selector);
        if (fecha == null && obligatorio) return anotar(campo, nombre, Lectura.falla(Lecturas.falta(nombre)));
        return anotar(campo, nombre, Lectura.de(fecha == null ? null : fecha.toString()));
    }

    /** Un combo obligatorio: vacío es que el catálogo no llegó o no hay nada cargado (sin salas, sin películas). */
    public <T> T elegido(JComboBox<Opcion<T>> combo, String nombre) {
        T valor = Campos.elegido(combo);
        return anotar(combo, nombre, valor == null ? Lectura.falla(Lecturas.falta(nombre)) : Lectura.de(valor));
    }

    /**
     * Para lo que no es un texto, como un grupo de casillas: {@code completo} dice si se llenó. Es obligatoriedad
     * (que se haya elegido algo), no una regla sobre qué se eligió.
     */
    public void exigir(boolean completo, JComponent campo, String nombre) {
        anotar(campo, nombre, completo ? Lectura.de(Boolean.TRUE) : Lectura.falla(Lecturas.falta(nombre)));
    }

    /** Para que un error del backend que nombra algo con otra palabra ("la vigencia") marque ese campo. */
    public void alMencionar(String palabra, JComponent campo) {
        nombrados.add(new Nombrado(palabra.toLowerCase(Locale.ROOT), campo));
    }

    public void alMencionar(String palabra, JDateChooser selector) {
        alMencionar(palabra, editor(selector));
    }

    private <T> T anotar(JComponent campo, String nombre, Lectura<T> lectura) {
        Marcas.desmarcar(campo);
        nombrados.add(new Nombrado(primeraPalabra(nombre), campo));
        if (!lectura.valida()) fallas.add(new Falla(campo, lectura.error()));
        return lectura.valor();
    }

    // "Precio base *" → "precio": es la palabra que usan los mensajes del backend ("El precio tiene que ser mayor a cero").
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
        fallas.forEach(f -> Marcas.marcar(f.campo()));
        mostrar(fallas.stream().map(Falla::texto).distinct().collect(Collectors.toList()));
        fallas.get(0).campo().requestFocusInWindow();
        return false;
    }

    public List<String> errores() {
        return fallas.stream().map(Falla::texto).toList();
    }

    /**
     * El rechazo del backend, tal cual llegó. Si nombra uno de los campos leídos, se marca: "El precio debe ser mayor
     * a cero" marca el precio. Es solo una ayuda para encontrarlo; el texto es el del backend. Lo que no es del
     * formulario (ver {@link #esDelFormulario}) va al diálogo de errores globales.
     */
    public void mostrarError(ErrorApi error) {
        if (error.esSesionVencida()) return;
        if (!esDelFormulario(error)) {
            Mensajes.error(mensaje, error);
            return;
        }
        mostrar(List.of(error.getMessage()));
        campoNombrado(error.getMessage()).ifPresent(Marcas::marcar);
    }

    /**
     * Si el rechazo se corrige tocando este formulario. Un 400 o un 404 siempre: es lo que se mandó. Un 409 solo si
     * nombra uno de sus campos: "Ya existe una sala con ese nombre" se arregla cambiando el nombre, pero que otro
     * pedido haya cambiado la reserva en el medio no lo arregla ningún campo. Sin conexión o un 500, nunca.
     */
    public boolean esDelFormulario(ErrorApi error) {
        return error.esDelFormulario() || (error.esConflicto() && campoNombrado(error.getMessage()).isPresent());
    }

    private Optional<JComponent> campoNombrado(String rechazo) {
        String texto = rechazo.toLowerCase(Locale.ROOT);
        return nombrados.stream()
                .filter(n -> !n.palabra().isEmpty()
                        && Pattern.compile("\\b" + Pattern.quote(n.palabra()) + "\\b",
                        Pattern.UNICODE_CHARACTER_CLASS).matcher(texto).find())
                .map(Nombrado::campo)
                .findFirst();
    }

    private void mostrar(List<String> textos) {
        // Sin ancho fijo: la etiqueta es de Componentes.texto y corta línea al ancho del formulario.
        mensaje.setText("<html>" + textos.stream().map(Formato::escapar).collect(Collectors.joining("<br>"))
                + "</html>");
    }

    private static JComponent editor(JDateChooser selector) {
        return (JComponent) selector.getDateEditor().getUiComponent();
    }
}
