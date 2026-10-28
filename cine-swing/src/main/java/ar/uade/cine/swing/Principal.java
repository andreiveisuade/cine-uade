package ar.uade.cine.swing;

import ar.uade.cine.swing.api.ApiSesion;
import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.pantallas.usuarios.VentanaLogin;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.util.prefs.Preferences;

// Arranca el panel de escritorio y lo alterna con el login: un 401 en cualquier pantalla vuelve al login.
// Las credenciales viven solo en memoria: una sesión rechazada no tiene qué rescatar, solo volver a pedirlas.
public final class Principal {

    // En la compu del encargado, no en el repo: el último servidor elegido.
    private final Preferences preferencias = Preferences.userNodeForPackage(Principal.class);
    private ClienteHttp http;
    private Apis apis;
    private VentanaPrincipal panel;

    public static void main(String[] args) {
        // Antes de crear cualquier componente: los que ya existen se quedan con los colores del tema anterior.
        if ("claro".equalsIgnoreCase(System.getProperty("cine.tema"))) {
            FlatLightLaf.setup();
        } else {
            FlatDarkLaf.setup();
        }
        SwingUtilities.invokeLater(() -> new Principal().mostrarLogin(null));
    }

    private void mostrarLogin(String aviso) {
        new VentanaLogin(preferencias, this::conectar, aviso, this::abrirPanel).setVisible(true);
    }

    private ApiSesion conectar(Servidor servidor) {
        ClienteHttp nuevo = new ClienteHttp(servidor.url());
        nuevo.alVencerSesion(() -> SwingUtilities.invokeLater(() -> {
            // Varias pantallas reciben el 401 a la vez: solo la primera cierra; el de una sesión vieja, ninguna.
            if (panel == null || http != nuevo) return;
            panel.dispose();
            panel = null;
            mostrarLogin("La sesión dejó de valer: volvé a ingresar");
        }));
        http = nuevo;
        apis = Apis.sobre(nuevo);
        return apis.sesion();
    }

    private void abrirPanel(Servidor servidor, Empleado empleado) {
        panel = new VentanaPrincipal(apis, servidor, empleado, () -> {
            http.olvidarCredenciales();
            panel.dispose();
            panel = null;
            mostrarLogin(null);
        });
        panel.setVisible(true);
    }
}
