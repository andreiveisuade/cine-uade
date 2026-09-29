package ar.uade.cine.swing.comun;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Component;

// El borde rojo de un campo con error, que se va solo apenas se corrige; lo pone Validacion.
/** Un texto, un combo o un spinner llevan el outline de error de FlatLaf; un panel de casillas, una línea roja. */
final class Marcas {

    private static final String OUTLINE = "JComponent.outline";
    private static final String ESCUCHA = "validacion.escucha";
    private static final String BORDE_ORIGINAL = "validacion.borde";

    private Marcas() {
    }

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
