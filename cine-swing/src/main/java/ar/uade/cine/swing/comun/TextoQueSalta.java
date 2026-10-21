package ar.uade.cine.swing.comun;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

// Una etiqueta HTML que corta línea al ancho que le da el layout; la crea Componentes.texto.
final class TextoQueSalta extends JLabel {

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
