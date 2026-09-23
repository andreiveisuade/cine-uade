package ar.uade.cine.swing.comun;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Un panel que sigue el ancho del scroll en vez de estirarlo: sin esto, lo más ancho del contenido (un título largo,
 * una nota, un combo) empuja todo hacia afuera y aparece la barra horizontal. {@code llenarAlto} lo estira hasta el
 * fondo cuando el contenido es más corto que el scroll, para que un recuadro llegue abajo como la tabla de al lado.
 */
public final class AlAnchoDelVisor extends JPanel implements Scrollable {

    private final boolean llenarAlto;

    public AlAnchoDelVisor(LayoutManager layout, boolean llenarAlto) {
        super(layout);
        this.llenarAlto = llenarAlto;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
        return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return llenarAlto && getParent() instanceof JViewport visor && visor.getHeight() > getPreferredSize().height;
    }
}
