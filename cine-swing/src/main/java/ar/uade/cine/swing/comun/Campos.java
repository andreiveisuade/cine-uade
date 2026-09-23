package ar.uade.cine.swing.comun;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Lee lo tipeado sin validarlo: lo vacío o lo que no es número sale null y viaja así, para que el rechazo y su mensaje
 * los dé el gestor del backend y no una copia de la regla acá.
 */
public final class Campos {

    private Campos() {
    }

    public static String texto(JTextComponent campo) {
        String valor = campo.getText().trim();
        return valor.isEmpty() ? null : valor;
    }

    public static Integer entero(JTextComponent campo) {
        try {
            return Integer.valueOf(campo.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double decimal(JTextComponent campo) {
        try {
            return Double.valueOf(campo.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "8,10,12" → [8, 10, 12]; lo que no es número se descarta. */
    public static List<Integer> enteros(JTextComponent campo) {
        return Arrays.stream(campo.getText().split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .map(Integer::valueOf)
                .toList();
    }

    /** "a1, B2" → ["A1", "B2"]. */
    public static List<String> codigos(JTextComponent campo) {
        return Arrays.stream(campo.getText().split(","))
                .map(s -> s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static <T> T elegido(JComboBox<Opcion<T>> combo) {
        Object item = combo.getSelectedItem();
        return item == null ? null : ((Opcion<T>) item).valor();
    }

    /** Elige la opción con ese valor, si está; si no, deja la que había. */
    public static <T> void elegir(JComboBox<Opcion<T>> combo, T valor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (java.util.Objects.equals(combo.getItemAt(i).valor(), valor)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }
}
