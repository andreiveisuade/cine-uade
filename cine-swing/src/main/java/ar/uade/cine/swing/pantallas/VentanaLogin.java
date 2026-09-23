package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.Empleado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Consumer;

public final class VentanaLogin extends JFrame {

    private final ApiHttp api;
    private final Consumer<Empleado> alIngresar;
    private final JTextField email = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JLabel mensaje = new JLabel(" ");
    private final JButton ingresar = new JButton("Ingresar");

    public VentanaLogin(ApiHttp api, String aviso, Consumer<Empleado> alIngresar) {
        super("Cine UADE · Encargado");
        this.api = api;
        this.alIngresar = alIngresar;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JLabel titulo = new JLabel("CINE UADE");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        mensaje.setForeground(Colores.error());
        if (aviso != null) mensaje.setText(aviso);

        Componentes.Formulario formulario = new Componentes.Formulario()
                .ancho(titulo)
                .ancho(Componentes.nota("Panel del encargado. Servidor: " + api.urlBase()))
                .campo("Email", email)
                .campo("Contraseña", password)
                .ancho(mensaje)
                .ancho(ingresar);

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        contenido.add(formulario);
        setContentPane(contenido);

        ingresar.addActionListener(e -> ingresar());
        getRootPane().setDefaultButton(ingresar);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        String correo = email.getText().trim();
        String clave = new String(password.getPassword());
        ingresar.setEnabled(false);
        mensaje.setText(" ");
        Tarea.ejecutar(this, () -> api.login(correo, clave), empleado -> {
            dispose();
            alIngresar.accept(empleado);
        }, error -> {
            // En el login, el 401 es "email o contraseña incorrectos": va al lado del formulario, no en un diálogo.
            ingresar.setEnabled(true);
            mensaje.setText(error.getMessage());
            password.selectAll();
            password.requestFocusInWindow();
        });
    }
}
