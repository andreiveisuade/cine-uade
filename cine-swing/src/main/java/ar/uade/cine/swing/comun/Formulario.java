package ar.uade.cine.swing.comun;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridBagLayoutInfo;
import java.awt.Insets;

// Formulario de dos columnas, etiqueta y campo, alineadas para leerse de un vistazo; se arma encadenando.
public final class Formulario extends JPanel {

    private int fila;

    public Formulario() {
        super(new AchicaLosCampos());
    }

    public Formulario campo(String etiqueta, JComponent campo) {
        GridBagConstraints izquierda = new GridBagConstraints();
        izquierda.gridx = 0;
        izquierda.gridy = fila;
        izquierda.anchor = GridBagConstraints.NORTHWEST;
        izquierda.insets = new Insets(4, 0, 4, 8);
        add(new JLabel(etiqueta), izquierda);
        GridBagConstraints derecha = new GridBagConstraints();
        derecha.gridx = 1;
        derecha.gridy = fila++;
        derecha.weightx = 1;
        derecha.fill = GridBagConstraints.HORIZONTAL;
        derecha.insets = new Insets(4, 0, 4, 0);
        add(campo, derecha);
        return this;
    }

    /** Un campo que hay que completar: el asterisco lo avisa antes de que el envío lo rechace. */
    public Formulario obligatorio(String etiqueta, JComponent campo) {
        return campo(etiqueta + " *", campo);
    }

    public Formulario ancho(JComponent componente) {
        GridBagConstraints todo = new GridBagConstraints();
        todo.gridx = 0;
        todo.gridy = fila++;
        todo.gridwidth = 2;
        todo.weightx = 1;
        todo.fill = GridBagConstraints.HORIZONTAL;
        todo.insets = new Insets(4, 0, 4, 0);
        add(componente, todo);
        return this;
    }

    /**
     * Cuando el formulario no entra a lo ancho, GridBagLayout pasa <i>todos</i> los componentes a su tamaño
     * mínimo: las etiquetas se amontonan y los campos quedan de dos letras. Midiendo siempre con el preferido,
     * la diferencia la absorbe la columna con peso, que es la de los campos: un título largo al editar se ve
     * cortado adentro de su campo en vez de empujar el formulario fuera de la vista.
     */
    private static final class AchicaLosCampos extends GridBagLayout {

        @Override
        protected GridBagLayoutInfo getLayoutInfo(Container padre, int medida) {
            return super.getLayoutInfo(padre, PREFERREDSIZE);
        }
    }

    /** Empuja todo hacia arriba: sin esto GridBagLayout centra el formulario en vertical. */
    public Formulario cerrar() {
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = fila++;
        relleno.weighty = 1;
        add(Box.createGlue(), relleno);
        return this;
    }
}
