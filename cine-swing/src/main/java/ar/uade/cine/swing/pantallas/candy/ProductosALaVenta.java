package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.dto.candy.Producto;
import ar.uade.cine.swing.comun.AlAnchoDelVisor;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Formato.precio;

// Los productos del mostrador, uno por fila con su precio y cuántos se llevan; el total no sale de acá.
final class ProductosALaVenta extends JScrollPane {

    private final JPanel filas = new AlAnchoDelVisor(new GridBagLayout(), false);
    private final Cantidades cantidades = new Cantidades();

    ProductosALaVenta() {
        setViewportView(filas);
    }

    void mostrar(List<Producto> productos) {
        filas.removeAll();
        cantidades.olvidar();
        if (productos.isEmpty()) {
            filas.add(Componentes.nota("No hay nada a la venta. Cargá productos en la carta."));
        }
        int fila = 0;
        for (Producto p : productos) {
            Component[] celdas = {nombre(p), new JLabel(precio(p.precio())), cantidades.nueva(p.id())};
            for (int columna = 0; columna < celdas.length; columna++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = columna;
                c.gridy = fila;
                c.weightx = columna == 0 ? 1 : 0;
                c.fill = columna == 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
                c.anchor = columna >= 1 ? GridBagConstraints.EAST : GridBagConstraints.WEST;
                c.insets = new Insets(4, 8, 4, 8);
                filas.add(celdas[columna], c);
            }
            fila++;
        }
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = fila;
        relleno.weighty = 1;
        filas.add(new JLabel(), relleno);
        filas.revalidate();
        filas.repaint();
    }

    /** productoId → cantidad, solo lo que lleva al menos una unidad. */
    Map<Integer, Integer> elegidas() {
        return cantidades.elegidas();
    }

    /** Después de cobrar: la próxima venta arranca de cero. */
    void ponerEnCero() {
        cantidades.ponerEnCero();
    }

    private static JPanel nombre(Producto p) {
        JLabel nombre = new JLabel(p.nombre());
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD));
        JPanel producto = new JPanel(new BorderLayout());
        producto.add(nombre, BorderLayout.NORTH);
        if (p.esCombo()) {
            // Debajo del nombre y sin ancho propio: un combo largo se corta con "…" en vez de empujar el precio y la
            // cantidad fuera de la vista.
            JLabel trae = new JLabel(PantallaCandy.componentesDe(p));
            trae.setForeground(Colores.secundario());
            trae.setToolTipText(PantallaCandy.componentesDe(p));
            trae.setPreferredSize(new Dimension(0, trae.getPreferredSize().height));
            producto.add(trae, BorderLayout.CENTER);
        }
        return producto;
    }
}
