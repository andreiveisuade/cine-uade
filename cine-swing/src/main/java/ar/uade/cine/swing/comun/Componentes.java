package ar.uade.cine.swing.comun;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridBagLayoutInfo;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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

    /**
     * Un JLabel con HTML no corta línea solo: informa el ancho de todo el texto en un renglón. Este mide la altura
     * que necesita al ancho que el layout le dio, y pide re-layout cuando ese ancho cambia.
     */
    private static final class TextoQueSalta extends JLabel {

        private static final int ANCHO_INICIAL = 420;
        private int anchoMedido = -1;

        TextoQueSalta(String html) {
            super(html);
            setVerticalAlignment(TOP);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (getWidth() != anchoMedido) SwingUtilities.invokeLater(TextoQueSalta.this::revalidate);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension natural = super.getPreferredSize();
            View vista = (View) getClientProperty(BasicHTML.propertyKey);
            int ancho = getWidth() > 0 ? getWidth() : ANCHO_INICIAL;
            if (vista == null || natural.width <= ancho) return natural;
            anchoMedido = ancho;
            Insets bordes = getInsets();
            vista.setSize(ancho - bordes.left - bordes.right, 0);
            int alto = (int) Math.ceil(vista.getPreferredSpan(View.Y_AXIS)) + bordes.top + bordes.bottom;
            return new Dimension(ancho, alto);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
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

    /** Formulario de dos columnas, etiqueta y campo, con las etiquetas alineadas para que se lea de un vistazo. */
    public static final class Formulario extends JPanel {

        private int fila;

        public Formulario() {
            super(new AchicaLosCampos());
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

        /** Un campo que hay que completar: el asterisco lo avisa antes de que el envío lo rechace. */
        public Formulario obligatorio(String etiqueta, JComponent campo) {
            return campo(etiqueta + " *", campo);
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

        /**
         * Cuando el formulario no entra a lo ancho, GridBagLayout pasa <i>todos</i> los componentes a su tamaño
         * mínimo: las etiquetas se amontonan y los campos quedan de dos letras. Midiendo siempre con el preferido,
         * la diferencia la absorbe la columna con peso, que es la de los campos: un título largo al editar se ve
         * cortado adentro de su campo en vez de empujar el formulario fuera de la vista.
         */
        private static final class AchicaLosCampos extends GridBagLayout {

            @Override
            protected GridBagLayoutInfo getLayoutInfo(Container padre, int medida) {
                return super.getLayoutInfo(padre, PREFERREDSIZE);
            }
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
