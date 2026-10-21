package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.pantallas.usuarios.VentanaLogin;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;

// Arranca el panel de escritorio y lo alterna con el login: un 401 en cualquier pantalla vuelve al login.
// Las credenciales viven solo en memoria: una sesión rechazada no tiene qué rescatar, solo volver a pedirlas.
public final class Principal {

    private final ClienteHttp http = new ClienteHttp(ClienteHttp.urlConfigurada());
    private final Apis apis = Apis.sobre(http);
    private VentanaPrincipal panel;

    public static void main(String[] args) {
        // Antes de crear cualquier componente: los que ya existen se quedan con los colores del tema anterior.
        if ("claro".equalsIgnoreCase(System.getProperty("cine.tema"))) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new Principal().arrancar());
    }

    private void arrancar() {
        http.alVencerSesion(() -> SwingUtilities.invokeLater(() -> {
            // Varias pantallas pueden recibir el 401 a la vez: solo la primera cierra el panel.
            if (panel == null) return;
            panel.dispose();
            panel = null;
            mostrarLogin("La sesión dejó de valer. Volvé a ingresar.");
        }));
        mostrarLogin(null);
    }

    private void mostrarLogin(String aviso) {
        new VentanaLogin(apis.sesion(), http.urlBase(), aviso, this::abrirPanel).setVisible(true);
    }

    private void abrirPanel(Empleado empleado) {
        panel = new VentanaPrincipal(apis, http.urlBase(), empleado, () -> {
            http.olvidarCredenciales();
            panel.dispose();
            panel = null;
            mostrarLogin(null);
        });
        panel.setVisible(true);
    }
}
