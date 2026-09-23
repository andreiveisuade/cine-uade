package ar.uade.cine.swing.pantallas;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.List;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

/** Los siete días como casillas. Ninguna marcada no es "ningún día": el backend lo lee como todos. */
final class SelectorDias extends JPanel {

    static final List<String> DIAS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY",
            "SUNDAY");

    private final List<JCheckBox> casillas = DIAS.stream().map(d -> new JCheckBox(etiqueta(d).substring(0, 3))).toList();

    SelectorDias() {
        // Dos filas: las siete en una no entran en el ancho de un formulario lateral.
        super(new GridLayout(2, 4, 2, 0));
        casillas.forEach(this::add);
    }

    List<String> elegidos() {
        return DIAS.stream().filter(d -> casillas.get(DIAS.indexOf(d)).isSelected()).toList();
    }

    void alCambiar(Runnable accion) {
        casillas.forEach(c -> c.addActionListener(e -> accion.run()));
    }

    static String resumen(List<String> dias) {
        if (dias == null || dias.isEmpty()) return "todos";
        return String.join(", ", dias.stream().map(d -> etiqueta(d).substring(0, 3)).toList());
    }
}
