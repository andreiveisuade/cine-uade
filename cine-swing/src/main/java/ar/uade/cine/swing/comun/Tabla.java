package ar.uade.cine.swing.comun;

import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.Getter;
import lombok.experimental.Accessors;

// Una JTable sobre una lista de records; cada columna saca su texto de la fila, sin TableModel por pantalla.
// El aspecto (filas, zebra, encabezado) es del tema; acá, lo que depende de la columna y el aviso de tabla vacía.
public final class Tabla<T> {

    public static final int ALTO_FILA = 30;
    // El encabezado de columnas más el borde del scroll: lo que hay que sumar para que no aparezca la barra.
    private static final int ALTO_ENCABEZADO = 34;

    /** El alto de scroll que muestra {@code filas} filas enteras, para una tabla corta que no debe scrollear. */
    public static int altoPara(int filas) {
        return ALTO_ENCABEZADO + ALTO_FILA * filas;
    }

    /** {@code color}: si viene, la celda es un estado (pagada, vencida, publicada) y se dibuja como pastilla. */
    public record Columna<T>(String titulo, Function<T, Object> valor, boolean aLaDerecha, int ancho,
                             Function<T, Color> color) {

        public static <T> Columna<T> de(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, false, 0, null);
        }

        public static <T> Columna<T> numero(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, true, 0, null);
        }

        public Columna<T> ancho(int pixeles) {
            return new Columna<>(titulo, valor, aLaDerecha, pixeles, color);
        }

        public Columna<T> comoEstado(Function<T, Color> segunFila) {
            return new Columna<>(titulo, valor, aLaDerecha, ancho, segunFila);
        }
    }

    private final List<Columna<T>> columnas;
    private final List<T> filas = new ArrayList<>();
    private final Modelo modelo = new Modelo();
    @Getter
    @Accessors(fluent = true)
    private final JTable tabla;
    private String vacia;

    @SafeVarargs
    public Tabla(Columna<T>... columnas) {
        this.columnas = List.of(columnas);
        // Después de las columnas: la JTable le pregunta al modelo cuántas hay apenas se crea.
        this.tabla = new JTable(modelo) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0 && vacia != null) pintarVacia(g, this);
            }
        };
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(false);
        tabla.setRowHeight(ALTO_FILA);
        TableCellRenderer encabezado = tabla.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < columnas.length; i++) {
            Columna<T> columna = columnas[i];
            int alineacion = columna.aLaDerecha() ? JLabel.RIGHT : JLabel.LEFT;
            TableColumn deLaTabla = tabla.getColumnModel().getColumn(i);
            deLaTabla.setCellRenderer(new Celda(columna, alineacion));
            // El título se alinea como sus datos: los importes, a la derecha también arriba.
            deLaTabla.setHeaderRenderer((t, valor, elegida, foco, fila, col) -> {
                Component celda = encabezado.getTableCellRendererComponent(t, valor, elegida, foco, fila, col);
                if (celda instanceof JLabel etiqueta) etiqueta.setHorizontalAlignment(alineacion);
                return celda;
            });
            if (columna.ancho() > 0) deLaTabla.setPreferredWidth(columna.ancho());
        }
    }

    /** Lo que se lee cuando no hay filas, en vez de un rectángulo vacío: qué falta o qué hacer. */
    public Tabla<T> vacia(String aviso) {
        vacia = aviso;
        tabla.repaint();
        return this;
    }

    public void mostrar(List<T> nuevas) {
        filas.clear();
        filas.addAll(nuevas);
        modelo.fireTableDataChanged();
    }

    public Optional<T> seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? Optional.empty() : Optional.of(filas.get(fila));
    }

    public void alDobleClic(Consumer<T> accion) {
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) seleccionada().ifPresent(accion);
            }
        });
    }

    public JScrollPane conScroll() {
        return new JScrollPane(tabla);
    }

    private void pintarVacia(Graphics g, JTable tabla) {
        Graphics2D g2 = (Graphics2D) g.create();
        FlatUIUtils.setRenderingHints(g2);
        g2.setColor(Colores.secundario());
        g2.setFont(tabla.getFont());
        FontMetrics medida = g2.getFontMetrics();
        Rectangle visible = tabla.getVisibleRect();
        g2.drawString(vacia, visible.x + (visible.width - medida.stringWidth(vacia)) / 2,
                visible.y + Componentes.MARGEN + medida.getAscent());
        g2.dispose();
    }

    private final class Celda extends DefaultTableCellRenderer {

        private final Columna<T> columna;
        private Color pastilla;

        Celda(Columna<T> columna, int alineacion) {
            this.columna = columna;
            setHorizontalAlignment(alineacion);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean elegida, boolean foco,
                                                       int fila, int col) {
            // DefaultTableCellRenderer recuerda el último color puesto: sin esto, el de una fila pasa a la siguiente.
            setForeground(null);
            super.getTableCellRendererComponent(tabla, valor, elegida, foco, fila, col);
            pastilla = columna.color() == null ? null : columna.color().apply(filas.get(fila));
            if (pastilla != null && !elegida) setForeground(pastilla);
            return this;
        }

        // Un ícono (la miniatura de un póster) se muestra como ícono; lo demás, como texto.
        @Override
        protected void setValue(Object valor) {
            setIcon(valor instanceof Icon icono ? icono : null);
            super.setValue(valor instanceof Icon ? null : valor);
        }

        // El fondo (zebra o selección) primero, encima la pastilla translúcida y arriba el texto de siempre.
        @Override
        protected void paintComponent(Graphics g) {
            if (pastilla == null) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            FlatUIUtils.setRenderingHints(g2);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            FontMetrics medida = g2.getFontMetrics(getFont());
            int alto = medida.getHeight() + Componentes.PEGADO;
            int ancho = medida.stringWidth(getText()) + 2 * Componentes.JUNTO;
            int x = getHorizontalAlignment() == RIGHT ? getWidth() - getInsets().right - ancho + Componentes.JUNTO
                    : getInsets().left - Componentes.JUNTO;
            g2.setColor(new Color(pastilla.getRed(), pastilla.getGreen(), pastilla.getBlue(), 40));
            g2.fillRoundRect(x, (getHeight() - alto) / 2, ancho, alto, alto, alto);
            g2.dispose();
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(true);
        }
    }

    private final class Modelo extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return columnas.size();
        }

        @Override
        public String getColumnName(int columna) {
            return columnas.get(columna).titulo();
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            return columnas.get(columna).valor().apply(filas.get(fila));
        }
    }
}
