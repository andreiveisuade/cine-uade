package ar.uade.cine.swing.comun;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Una JTable sobre una lista de records: cada columna dice cómo sacar su texto de la fila. Evita repetir en cada
 * pantalla un AbstractTableModel con su switch por índice de columna.
 */
public final class Tabla<T> {

    public record Columna<T>(String titulo, Function<T, Object> valor, boolean aLaDerecha, int ancho) {

        public static <T> Columna<T> de(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, false, 0);
        }

        public static <T> Columna<T> numero(String titulo, Function<T, Object> valor) {
            return new Columna<>(titulo, valor, true, 0);
        }

        public Columna<T> ancho(int pixeles) {
            return new Columna<>(titulo, valor, aLaDerecha, pixeles);
        }
    }

    private final List<Columna<T>> columnas;
    private final List<T> filas = new ArrayList<>();
    private final Modelo modelo = new Modelo();
    private final JTable tabla = new JTable(modelo);

    @SafeVarargs
    public Tabla(Columna<T>... columnas) {
        this.columnas = List.of(columnas);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(false);
        tabla.setRowHeight(26);
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 0; i < columnas.length; i++) {
            if (columnas[i].aLaDerecha()) tabla.getColumnModel().getColumn(i).setCellRenderer(derecha);
            if (columnas[i].ancho() > 0) tabla.getColumnModel().getColumn(i).setPreferredWidth(columnas[i].ancho());
        }
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

    public JTable tabla() {
        return tabla;
    }

    public JScrollPane conScroll() {
        return new JScrollPane(tabla);
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
