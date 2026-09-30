package ar.uade.cine.swing.pantallas.candy;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

// Una cantidad por producto, con su spinner; al backend viaja productoId → cantidad, en combos y ventas.
final class Cantidades {

    private final Map<Integer, JSpinner> spinners = new LinkedHashMap<>();

    /** El spinner del producto, en cero, listo para poner en su fila. */
    JSpinner nueva(int productoId) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
        spinner.setPreferredSize(new Dimension(70, spinner.getPreferredSize().height));
        spinners.put(productoId, spinner);
        return spinner;
    }

    /** Antes de volver a pintar las filas: los productos pueden haber cambiado. */
    void olvidar() {
        spinners.clear();
    }

    void ponerEnCero() {
        spinners.values().forEach(s -> s.setValue(0));
    }

    // Solo lo que lleva al menos una unidad: el backend recibe productoId → cantidad.
    Map<Integer, Integer> elegidas() {
        Map<Integer, Integer> elegidas = new LinkedHashMap<>();
        spinners.forEach((id, spinner) -> {
            int n = (Integer) spinner.getValue();
            if (n > 0) elegidas.put(id, n);
        });
        return elegidas;
    }
}
