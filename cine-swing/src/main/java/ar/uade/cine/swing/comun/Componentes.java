package ar.uade.cine.swing.comun;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

// Piezas de pantalla que se repiten, para que cada pantalla arme lo suyo y todas se vean igual.
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

    /**
     * Lo que ocupa el formulario al costado de una tabla, igual en todas las pantallas. A 960 px de ventana deja
     * para la tabla un poco más de lo que se lleva el formulario.
     */
    public static final int ANCHO_LATERAL = 340;

    /**
     * El formulario de al lado de la tabla: ancho fijo, la tabla toma el resto y el scroll es solo vertical. El
     * contenido se ajusta a ese ancho en vez de estirarlo, así un título largo al editar no lo corta a la derecha.
     */
    public static JScrollPane lateral(JComponent contenido) {
        AlAnchoDelVisor visor = new AlAnchoDelVisor(new BorderLayout(), true);
        visor.add(contenido, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(visor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(ANCHO_LATERAL, 0));
        return scroll;
    }

    public static JLabel subtitulo(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 15f));
        return etiqueta;
    }

    /** Texto gris que explica. Corta línea al ancho que le toque, en vez de estirar la ventana o cortarse. */
    public static JLabel nota(String texto) {
        JLabel etiqueta = texto(texto);
        etiqueta.setForeground(gris());
        etiqueta.setFont(etiqueta.getFont().deriveFont(12f));
        return etiqueta;
    }

    /** HTML que corta línea al ancho que le toque, con el color y la letra de siempre. */
    public static JLabel texto(String html) {
        return new TextoQueSalta("<html>" + html + "</html>");
    }

    public static Color gris() {
        return Colores.secundario();
    }

    public static JComponent izquierda(JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        return componente;
    }

    public static JPanel conBorde(JComponent contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Colores.borde()),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        panel.add(contenido);
        return panel;
    }

    /**
     * Una cifra grande con su título arriba, en un recuadro: el arqueo y los indicadores del planificador. {@code
     * detalle} y {@code variacion} son opcionales.
     */
    public static JPanel cifra(String titulo, String valor, String detalle, String variacion) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel arriba = new JLabel(titulo.toUpperCase());
        arriba.setForeground(gris());
        arriba.setFont(arriba.getFont().deriveFont(11f));
        JLabel numero = new JLabel(valor);
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(izquierda(arriba));
        panel.add(izquierda(numero));
        if (detalle != null) panel.add(izquierda(texto(detalle)));
        if (variacion != null) {
            JLabel cambio = new JLabel(variacion);
            cambio.setFont(cambio.getFont().deriveFont(11f));
            panel.add(izquierda(cambio));
        }
        return conBorde(panel);
    }

    /** Un importe en un renglón: el texto a la izquierda y el valor a la derecha; {@code fuerte}, para el total. */
    public static JPanel renglon(String texto, String valor, boolean fuerte) {
        JPanel fila = new JPanel(new BorderLayout());
        JLabel izquierda = new JLabel(texto);
        JLabel derecha = new JLabel(valor);
        if (fuerte) {
            izquierda.setFont(izquierda.getFont().deriveFont(Font.BOLD, 16f));
            derecha.setFont(derecha.getFont().deriveFont(Font.BOLD, 16f));
        }
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(derecha, BorderLayout.EAST);
        fila.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
        return fila;
    }
}
