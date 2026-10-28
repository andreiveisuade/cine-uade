package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

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
// Solo formato y obligatoriedad; las reglas de negocio las decide el backend y su mensaje se muestra tal cual.
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

    public String url(JTextComponent campo, String nombre, boolean obligatorio) {
        return anotar(campo, nombre, Lecturas.leerUrl(campo.getText(), nombre, obligatorio));
    }

    /** La fecha en ISO, o null; recibe el Adapter {@link CampoFecha}: Validacion no conoce JCalendar. */
    public String fecha(CampoFecha selector, String nombre, boolean obligatorio) {
        LocalDate fecha = selector.valor();
        JComponent campo = selector.editor();
        if (fecha == null && obligatorio) return anotar(campo, nombre, ValorLeido.falla(Lecturas.falta(nombre)));
        return anotar(campo, nombre, ValorLeido.de(fecha == null ? null : fecha.toString()));
    }

    /** Un combo obligatorio: vacío es que el catálogo no llegó o no hay nada cargado (sin salas, sin películas). */
    public <T> T elegido(JComboBox<Opcion<T>> combo, String nombre) {
        T valor = Campos.elegido(combo);
        return anotar(combo, nombre, valor == null ? ValorLeido.falla(Lecturas.falta(nombre)) : ValorLeido.de(valor));
    }

    /** Para lo que no es texto (casillas): {@code completo} dice si se eligió algo. */
    public void exigir(boolean completo, JComponent campo, String nombre) {
        anotar(campo, nombre, completo ? ValorLeido.de(Boolean.TRUE) : ValorLeido.falla(Lecturas.falta(nombre)));
    }

    /** Para que un error del backend que nombra algo con otra palabra ("la vigencia") marque ese campo. */
    public void alMencionar(String palabra, JComponent campo) {
        nombrados.add(new Nombrado(palabra.toLowerCase(Locale.ROOT), campo));
    }

    public void alMencionar(String palabra, CampoFecha selector) {
        alMencionar(palabra, selector.editor());
    }

    private <T> T anotar(JComponent campo, String nombre, ValorLeido<T> lectura) {
        Marcas.desmarcar(campo);
        nombrados.add(new Nombrado(primeraPalabra(nombre), campo));
        if (!lectura.valida()) fallas.add(new Falla(campo, lectura.error()));
        return lectura.valor();
    }

    // "Precio base *" → "precio": la palabra de los mensajes del backend.
    private static String primeraPalabra(String nombre) {
        String limpio = nombre.replace("*", "").trim().toLowerCase(Locale.ROOT);
        int espacio = limpio.indexOf(' ');
        return espacio < 0 ? limpio : limpio.substring(0, espacio);
    }

    /** Si todo se leyó, borra el mensaje y devuelve true; si no, marca los campos y enfoca el primero. */
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

    /** Muestra el rechazo del backend tal cual; si nombra un campo leído, lo marca. */
    public void mostrarError(ErrorApi error) {
        if (error.esSesionVencida()) return;
        if (!esDelFormulario(error)) {
            Mensajes.error(mensaje, error);
            return;
        }
        mostrar(List.of(error.getMessage()));
        campoNombrado(error.getMessage()).ifPresent(Marcas::marcar);
    }

    /** Un 400 o 404 se corrige en el formulario; un 409 solo si nombra un campo; conexión o 500, nunca. */
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
}
