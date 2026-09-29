package ar.uade.cine.swing.comun;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

// Un panel que apila de arriba abajo, todo alineado a la izquierda, como piden los paneles de detalle.
/**
 * BoxLayout centra a quien no dice su alineación, y cada {@code add} tenía que acordarse de pedir la izquierda: acá se
 * pide una sola vez.
 */
public class Pila extends JPanel {

    public Pila() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public Pila agregar(JComponent... componentes) {
        for (JComponent componente : componentes) add(Componentes.izquierda(componente));
        return this;
    }

    /** Aire entre bloques, en píxeles. */
    public Pila espacio(int alto) {
        add(Box.createVerticalStrut(alto));
        return this;
    }

    /** Empuja lo apilado hacia arriba cuando sobra alto. */
    public Pila relleno() {
        add(Box.createVerticalGlue());
        return this;
    }
}
