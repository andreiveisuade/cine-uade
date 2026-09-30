package ar.uade.cine.swing.comun;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

// Un FlowLayout que informa el alto de todas sus líneas: así una barra de filtros angosta no pierde nada.
/**
 * FlowLayout que pide la altura de todas las líneas que arma. El de Swing reparte en varias líneas pero informa
 * la altura de una sola, así que en el norte de un BorderLayout una barra de filtros angosta pierde lo que no entra.
 */
public final class FlujoConSalto extends FlowLayout {

    public FlujoConSalto() {
        super(FlowLayout.LEFT, 6, 2);
    }

    @Override
    public Dimension preferredLayoutSize(Container destino) {
        return medir(destino, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container destino) {
        Dimension minimo = medir(destino, false);
        minimo.width -= getHgap() + 1;
        return minimo;
    }

    private Dimension medir(Container destino, boolean preferido) {
        synchronized (destino.getTreeLock()) {
            int ancho = destino.getSize().width;
            Container padre = destino;
            // Recién creado todavía no tiene ancho: se usa el del primer contenedor que ya lo tenga.
            while (ancho == 0 && padre.getParent() != null) {
                padre = padre.getParent();
                ancho = padre.getSize().width;
            }
            if (ancho == 0) ancho = Integer.MAX_VALUE;

            Insets bordes = destino.getInsets();
            int disponible = ancho - (bordes.left + bordes.right + getHgap() * 2);
            Dimension total = new Dimension(0, 0);
            int anchoLinea = 0;
            int altoLinea = 0;
            for (Component c : destino.getComponents()) {
                if (!c.isVisible()) continue;
                Dimension d = preferido ? c.getPreferredSize() : c.getMinimumSize();
                if (anchoLinea + d.width > disponible && anchoLinea > 0) {
                    cerrarLinea(total, anchoLinea, altoLinea);
                    anchoLinea = 0;
                    altoLinea = 0;
                }
                if (anchoLinea != 0) anchoLinea += getHgap();
                anchoLinea += d.width;
                altoLinea = Math.max(altoLinea, d.height);
            }
            cerrarLinea(total, anchoLinea, altoLinea);
            total.width += bordes.left + bordes.right + getHgap() * 2;
            total.height += bordes.top + bordes.bottom + getVgap() * 2;
            // Dentro de un scroll, pedir todo el ancho impediría que el scroll se achique.
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, destino) != null && destino.isValid()) {
                total.width -= getHgap() + 1;
            }
            return total;
        }
    }

    private void cerrarLinea(Dimension total, int ancho, int alto) {
        total.width = Math.max(total.width, ancho);
        if (total.height > 0) total.height += getVgap();
        total.height += alto;
    }
}
