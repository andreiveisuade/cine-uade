package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiSesion;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.prefs.Preferences;

// La ventana del ingreso; el formulario es PanelIngreso, que se prueba sin ventana.
public final class VentanaLogin extends JFrame {

    public VentanaLogin(Preferences preferencias, Function<Servidor, ApiSesion> conectar, String aviso,
                        BiConsumer<Servidor, Empleado> alIngresar) {
        super("Cine UADE · Ingreso");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        PanelIngreso panel = new PanelIngreso(preferencias, conectar, aviso, (servidor, empleado) -> {
            dispose();
            alIngresar.accept(servidor, empleado);
        });
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.botonIngresar());
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }
}
