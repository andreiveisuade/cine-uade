package ar.uade.cine.swing.comun;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

/**
 * Los siete días como casillas, y el único lugar que sabe cómo se nombran y abrevian: grillas, promociones y agenda
 * los muestran igual. Ninguna marcada no es "ningún día": el backend lo lee como todos.
 */
public final class SelectorDias extends JPanel {

    // Los nombres de DayOfWeek: es lo que viaja en `diasSemana`.
    public static final List<String> DIAS = Arrays.stream(DayOfWeek.values()).map(DayOfWeek::name).toList();

    private final List<JCheckBox> casillas = DIAS.stream().map(d -> new JCheckBox(abreviatura(d))).toList();

    public SelectorDias() {
        // Dos filas: las siete en una no entran en el ancho de un formulario lateral.
        super(new GridLayout(2, 4, 2, 0));
        casillas.forEach(this::add);
    }

    public List<String> elegidos() {
        return DIAS.stream().filter(d -> casillas.get(DIAS.indexOf(d)).isSelected()).toList();
    }

    public void limpiar() {
        casillas.forEach(c -> c.setSelected(false));
    }

    public void alCambiar(Runnable accion) {
        casillas.forEach(c -> c.addActionListener(e -> accion.run()));
    }

    /** "MONDAY" → "Lun". */
    public static String abreviatura(String dia) {
        return etiqueta(dia).substring(0, 3);
    }

    public static String abreviatura(DayOfWeek dia) {
        return abreviatura(dia.name());
    }

    /** Vacío o null es "todos", como lo lee el backend. */
    public static String resumen(List<String> dias) {
        if (dias == null || dias.isEmpty()) return "todos";
        return dias.stream().map(SelectorDias::abreviatura).collect(Collectors.joining(", "));
    }
}
