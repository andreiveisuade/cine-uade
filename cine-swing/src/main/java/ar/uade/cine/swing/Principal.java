package ar.uade.cine.swing;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Empleado;
import ar.uade.cine.swing.pantallas.VentanaLogin;
import ar.uade.cine.swing.pantallas.VentanaPrincipal;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;

/**
 * Arranca el panel del encargado de escritorio. Lleva el ida y vuelta entre login y panel: un 401 en cualquier
 * pantalla cierra el panel y reabre el login, igual que el evento {@code cine:sesion-vencida} del panel web.
 */
public final class Principal {

    private final ApiHttp api = new ApiHttp(ApiHttp.urlConfigurada());
    private VentanaPrincipal panel;

    public static void main(String[] args) {
        // Oscuro por defecto; -Dcine.tema=claro vuelve al claro. Antes de crear cualquier componente: los que ya
        // existen se quedan con los colores del tema anterior.
        if ("claro".equalsIgnoreCase(System.getProperty("cine.tema"))) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new Principal().arrancar());
    }

    private void arrancar() {
        api.alVencerSesion(() -> SwingUtilities.invokeLater(() -> {
            // Varias pantallas pueden recibir el 401 a la vez: solo la primera cierra el panel.
            if (panel == null) return;
            panel.dispose();
            panel = null;
            mostrarLogin("La sesión dejó de valer. Volvé a ingresar.");
        }));
        mostrarLogin(null);
    }

    private void mostrarLogin(String aviso) {
        new VentanaLogin(api, aviso, this::abrirPanel).setVisible(true);
    }

    private void abrirPanel(Empleado empleado) {
        panel = new VentanaPrincipal(api, empleado, () -> {
            api.olvidarCredenciales();
            panel.dispose();
            panel = null;
            mostrarLogin(null);
        });
        panel.setVisible(true);
    }
}
