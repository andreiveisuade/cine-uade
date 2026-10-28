package ar.uade.cine.swing.pantallas.produccion;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.util.prefs.Preferences;

// La ventana del estado de producción: no modal, para seguir el despertar sin trabar el login ni el panel.
public final class VentanaProduccion {

    private VentanaProduccion() {
    }

    public static void abrir(Component origen, Preferences preferencias) {
        JDialog ventana = new JDialog(SwingUtilities.getWindowAncestor(origen), "Estado de producción",
                Dialog.ModalityType.MODELESS);
        PanelProduccion panel = new PanelProduccion(preferencias);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        ventana.setContentPane(panel);
        ventana.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        ventana.pack();
        ventana.setLocationRelativeTo(origen);
        ventana.setVisible(true);
    }
}
