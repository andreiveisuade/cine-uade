package ar.uade.cine.swing.comun;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

// Piezas de pantalla que se repiten: el equivalente de admin/comun.jsx.
public final class Componentes {

    private Componentes() {
    }

    public static JPanel encabezado(String titulo, String descripcion) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(izquierda(etiqueta));
        if (descripcion != null) {
            panel.add(Box.createVerticalStrut(4));
            panel.add(izquierda(nota(descripcion)));
        }
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return panel;
    }

    public static JLabel subtitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 15f));
        return etiqueta;
    }

    /** Texto gris que explica; con HTML para que corte línea en vez de estirar la ventana. */
    public static JLabel nota(String texto) {
        JLabel etiqueta = new JLabel("<html><div style='width:420px'>" + texto + "</div></html>");
        etiqueta.setForeground(gris());
        etiqueta.setFont(etiqueta.getFont().deriveFont(12f));
        return etiqueta;
    }

    public static Color gris() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? color : Color.GRAY;
    }

    public static JComponent izquierda(JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        return componente;
    }

    public static JPanel conBorde(JComponent contenido) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null
                        ? UIManager.getColor("Component.borderColor") : Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.add(contenido);
        return panel;
    }

    /** Formulario de dos columnas, etiqueta y campo, que es lo que en el panel web arma un Stack de inputs. */
    public static final class Formulario extends JPanel {

        private int fila;

        public Formulario() {
            super(new GridBagLayout());
        }

        public Formulario campo(String etiqueta, JComponent campo) {
            GridBagConstraints izquierda = new GridBagConstraints();
            izquierda.gridx = 0;
            izquierda.gridy = fila;
            izquierda.anchor = GridBagConstraints.NORTHWEST;
            izquierda.insets = new Insets(4, 0, 4, 8);
            add(new JLabel(etiqueta), izquierda);
            GridBagConstraints derecha = new GridBagConstraints();
            derecha.gridx = 1;
            derecha.gridy = fila++;
            derecha.weightx = 1;
            derecha.fill = GridBagConstraints.HORIZONTAL;
            derecha.insets = new Insets(4, 0, 4, 0);
            add(campo, derecha);
            return this;
        }

        public Formulario ancho(JComponent componente) {
            GridBagConstraints todo = new GridBagConstraints();
            todo.gridx = 0;
            todo.gridy = fila++;
            todo.gridwidth = 2;
            todo.weightx = 1;
            todo.fill = GridBagConstraints.HORIZONTAL;
            todo.insets = new Insets(4, 0, 4, 0);
            add(componente, todo);
            return this;
        }

        /** Empuja todo hacia arriba: sin esto GridBagLayout centra el formulario en vertical. */
        public Formulario cerrar() {
            GridBagConstraints relleno = new GridBagConstraints();
            relleno.gridy = fila++;
            relleno.weighty = 1;
            add(Box.createGlue(), relleno);
            return this;
        }
    }
}
