package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;

/**
 * Dónde se muestra cada mensaje. Todo pasa por acá, así que cambiar la política toca este archivo:
 * <ul>
 *   <li><b>Error de un campo</b> (formato, obligatorio o un 400 del backend): en línea, junto al formulario y con el
 *   campo marcado. Lo hace {@link Validacion}; si el error no es de ese tipo, lo manda a {@link #error}.</li>
 *   <li><b>Error que no es de ningún campo</b> (sin conexión, 500, 409, 403, o un 400 en una pantalla sin
 *   formulario): {@link #error}, un diálogo modal con el texto del backend tal cual. El 401 no: vuelve al login.</li>
 *   <li><b>Acción destructiva o sin vuelta atrás</b>: {@link #confirmar} antes de mandar, con un botón que dice qué
 *   se va a hacer.</li>
 *   <li><b>Éxito</b>: {@link #exito}, en la barra de estado de la ventana, en verde, y se borra solo. Sin diálogo:
 *   no hay nada que decidir, y un clic de más en cada alta cansa.</li>
 * </ul>
 */
public final class Mensajes {

    private static final String BARRA = "cine.barraDeEstado";
    private static final int DURACION_EXITO_MS = 6000;

    private Mensajes() {
    }

    public static void error(Component origen, ErrorApi error) {
        if (error.esSesionVencida()) return;
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(origen), error.getMessage(),
                error.esSinConexion() ? "Sin conexión con el servidor" : "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** {@code si}: el botón que confirma, con el verbo de la acción ("Sí, borrar"). El otro es "Cancelar". */
    public static boolean confirmar(Component origen, String pregunta, String si) {
        return confirmar(origen, pregunta, si, "Cancelar");
    }

    /** Con el botón de no propio, para cuando la acción misma se llama "cancelar". */
    public static boolean confirmar(Component origen, String pregunta, String si, String no) {
        Object[] opciones = {si, no};
        return JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(origen), pregunta, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, opciones, no) == 0;
    }

    /** La barra de abajo de la ventana. Se crea una por ventana y {@link #exito} la encuentra desde cualquier hijo. */
    public static JLabel barraDeEstado(JRootPane raiz) {
        JLabel barra = new JLabel(" ");
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Colores.borde()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        raiz.putClientProperty(BARRA, barra);
        return barra;
    }

    public static void exito(Component origen, String mensaje) {
        JRootPane raiz = SwingUtilities.getRootPane(origen);
        if (raiz == null || !(raiz.getClientProperty(BARRA) instanceof JLabel barra)) return;
        barra.setForeground(Colores.exito());
        barra.setText(mensaje);
        // Un aviso nuevo reinicia la cuenta: el anterior no borra al que llegó después.
        if (barra.getClientProperty(BARRA) instanceof Timer anterior) anterior.stop();
        Timer borrar = new Timer(DURACION_EXITO_MS, e -> barra.setText(" "));
        borrar.setRepeats(false);
        barra.putClientProperty(BARRA, borrar);
        borrar.start();
    }
}
