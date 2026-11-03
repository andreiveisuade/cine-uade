package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;

// Dónde va cada mensaje: error de campo en línea, global en diálogo, éxito en la barra; la política vive acá.
public final class Mensajes {

    private static final String BARRA = "cine.barraDeEstado";
    private static final int DURACION_EXITO_MS = 6000;

    private Mensajes() {
    }

    public static void error(Component origen, ErrorApi error) {
        if (error.esSesionVencida()) return;
        mostrar(origen, new JOptionPane(error.getMessage(), JOptionPane.ERROR_MESSAGE),
                error.esSinConexion() ? "Sin conexión con el servidor" : "Error");
    }

    /** Un error de este lado y no del servidor: un archivo que no se pudo escribir, una importación que falló. */
    public static void error(Component origen, String mensaje) {
        error(origen, new ErrorApi(-1, mensaje));
    }

    /** {@code si}: el botón que confirma, con el verbo de la acción ("Sí, borrar"). El otro es "Cancelar". */
    public static boolean confirmar(Component origen, String pregunta, String si) {
        return confirmar(origen, pregunta, si, "Cancelar");
    }

    /** Con el botón de no propio, para cuando la acción misma se llama "cancelar". */
    public static boolean confirmar(Component origen, String pregunta, String si, String no) {
        Object[] opciones = {si, no};
        return si.equals(mostrar(origen, new JOptionPane(pregunta, JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, opciones, no), "Confirmar"));
    }

    /** Un formulario en diálogo con Aceptar y Cancelar; true si se aceptó. */
    public static boolean aceptar(Component origen, Object formulario, String titulo) {
        Object valor = mostrar(origen, new JOptionPane(formulario, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION), titulo);
        return valor instanceof Integer opcion && opcion == JOptionPane.OK_OPTION;
    }

    // Un gestor de ventanas en mosaico (AeroSpace) dejaba el diálogo en 80×33, sin contenido y sin poder
    // agrandarlo: el mínimo es lo que el contenido necesita, y redimensionable por si igual sale corto.
    private static Object mostrar(Component origen, JOptionPane panel, String titulo) {
        JDialog dialogo = panel.createDialog(SwingUtilities.getWindowAncestor(origen), titulo);
        dialogo.setResizable(true);
        dialogo.pack();
        dialogo.setMinimumSize(dialogo.getSize());
        dialogo.setVisible(true);
        dialogo.dispose();
        return panel.getValue();
    }

    /** La barra de abajo de la ventana. Se crea una por ventana y {@link #exito} la encuentra desde cualquier hijo. */
    public static JLabel barraDeEstado(JRootPane raiz) {
        JLabel barra = new JLabel(" ");
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Colores.borde()),
                BorderFactory.createEmptyBorder(6, Escala.APARTE, 6, Escala.APARTE)));
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
