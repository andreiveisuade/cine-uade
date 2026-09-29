package ar.uade.cine.swing.comun;

import javax.swing.JComboBox;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lo que se hace con un campo mientras se lo usa: qué se deja tipear, qué opción está elegida y cuándo reaccionar a
 * lo tipeado. Leer el valor para mandarlo es de {@link Validacion}, que separa dos cosas: el formato y la
 * obligatoriedad (un número mal tipeado, un obligatorio vacío) los revisa el cliente antes de mandar, porque no son
 * reglas del cine sino un pedido mal armado; las reglas de negocio (precio mayor a cero, rangos, R1 a R20) las
 * decide solo el backend, y su mensaje se muestra tal cual.
 */
public final class Campos {

    // Lo que se deja tipear en cada caso. El signo se deja: un negativo es un número; si vale lo dice el backend.
    private static final Pattern ENTERO_A_MEDIAS = Pattern.compile("-?\\d*");
    private static final Pattern DECIMAL_A_MEDIAS = Pattern.compile("-?\\d*([.,]\\d*)?");
    private static final Pattern LISTA_A_MEDIAS = Pattern.compile("[\\d,\\s]*");
    // Lo que tarda en dispararse una búsqueda desde que se deja de tipear: sin espera, "Matrix" son seis pedidos.
    private static final int ESPERA_AL_TIPEAR_MS = 250;

    private Campos() {
    }

    /** Solo deja tipear (o pegar) un entero. */
    public static <C extends JTextComponent> C soloEntero(C campo) {
        return soloSi(campo, ENTERO_A_MEDIAS);
    }

    /** Solo deja tipear un número con decimales, con coma o con punto. */
    public static <C extends JTextComponent> C soloDecimal(C campo) {
        return soloSi(campo, DECIMAL_A_MEDIAS);
    }

    /** Solo deja tipear números separados por coma, como "8, 10, 12". */
    public static <C extends JTextComponent> C soloListaDeEnteros(C campo) {
        return soloSi(campo, LISTA_A_MEDIAS);
    }

    // El patrón se prueba contra el texto como quedaría, no contra lo tipeado: así un pegado entero también pasa por acá.
    private static <C extends JTextComponent> C soloSi(C campo, Pattern permitido) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new SoloSi(campo, permitido));
        return campo;
    }

    private static final class SoloSi extends DocumentFilter {

        private final JTextComponent campo;
        private final Pattern permitido;

        SoloSi(JTextComponent campo, Pattern permitido) {
            this.campo = campo;
            this.permitido = permitido;
        }

        @Override
        public void insertString(FilterBypass fb, int desde, String texto, AttributeSet atributos)
                throws BadLocationException {
            replace(fb, desde, 0, texto, atributos);
        }

        @Override
        public void replace(FilterBypass fb, int desde, int largo, String texto, AttributeSet atributos)
                throws BadLocationException {
            String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nuevo = actual.substring(0, desde) + (texto == null ? "" : texto) + actual.substring(desde + largo);
            if (permitido.matcher(nuevo).matches()) {
                fb.replace(desde, largo, texto, atributos);
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(campo);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T elegido(JComboBox<Opcion<T>> combo) {
        Object item = combo.getSelectedItem();
        return item == null ? null : ((Opcion<T>) item).valor();
    }

    /** Reemplaza las opciones del combo. */
    public static <T> void llenar(JComboBox<Opcion<T>> combo, List<Opcion<T>> opciones) {
        combo.removeAllItems();
        opciones.forEach(combo::addItem);
    }

    /** Para un filtro: primero la opción sin valor ({@code todas}, "Todas" o "Todos"), que no filtra. */
    public static <T> void llenarConTodas(JComboBox<Opcion<T>> combo, String todas, List<Opcion<T>> opciones) {
        combo.removeAllItems();
        combo.addItem(new Opcion<>(null, todas));
        opciones.forEach(combo::addItem);
    }

    /** Elige la opción con ese valor, si está; si no, deja la que había. */
    public static <T> void elegir(JComboBox<Opcion<T>> combo, T valor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (Objects.equals(combo.getItemAt(i).valor(), valor)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Corre {@code accion} con cada cambio del texto, se tipee, se borre o se pegue. */
    public static void alCambiar(JTextComponent campo, Runnable accion) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accion.run();
            }
        });
    }

    /**
     * Corre {@code accion} cuando se deja de tipear. Devuelve el Timer para que un "Limpiar" que vacía el campo pueda
     * pararlo y buscar una sola vez.
     */
    public static Timer alDejarDeTipear(JTextComponent campo, Runnable accion) {
        Timer espera = new Timer(ESPERA_AL_TIPEAR_MS, e -> accion.run());
        espera.setRepeats(false);
        alCambiar(campo, espera::restart);
        return espera;
    }
}
