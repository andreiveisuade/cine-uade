package ar.uade.cine.swing.comun;

import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.function.Consumer;

// Los íconos de la cabecera, dibujados en vez de imágenes: nítidos en cualquier pantalla y con el color del tema.
public final class Iconos {

    private static final int LADO = 16;

    private Iconos() {
    }

    public static Icon sol() {
        return dibujado(g -> {
            g.draw(new Ellipse2D.Double(5, 5, 6, 6));
            for (int i = 0; i < 8; i++) {
                g.draw(new Line2D.Double(8, 1.5, 8, 3));
                g.rotate(Math.PI / 4, 8, 8);
            }
        });
    }

    public static Icon luna() {
        return dibujado(g -> {
            Area luna = new Area(new Ellipse2D.Double(2, 2, 12, 12));
            luna.subtract(new Area(new Ellipse2D.Double(6, 0, 11, 11)));
            g.fill(luna);
        });
    }

    public static Icon actividad() {
        return dibujado(g -> g.draw(linea(1, 8.5, 4.5, 8.5, 6.5, 3, 9.5, 13, 11.5, 8.5, 15, 8.5)));
    }

    public static Icon salir() {
        return dibujado(g -> {
            g.draw(linea(9, 2, 3, 2, 3, 14, 9, 14));
            g.draw(linea(7, 8, 15, 8));
            g.draw(linea(12, 5, 15, 8, 12, 11));
        });
    }

    private static Path2D linea(double... puntos) {
        Path2D linea = new Path2D.Double();
        linea.moveTo(puntos[0], puntos[1]);
        for (int i = 2; i < puntos.length; i += 2) linea.lineTo(puntos[i], puntos[i + 1]);
        return linea;
    }

    private static Icon dibujado(Consumer<Graphics2D> dibujo) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                FlatUIUtils.setRenderingHints(g2);
                g2.translate(x, y);
                g2.setColor(c.getForeground());
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                dibujo.accept(g2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return LADO;
            }

            @Override
            public int getIconHeight() {
                return LADO;
            }
        };
    }
}
