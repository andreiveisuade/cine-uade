package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;

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
    private final JLabel mensaje = Componentes.texto(" ");
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
                .obligatorio("Email", email)
                .obligatorio("Contraseña", password)
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
        Validacion v = new Validacion(mensaje);
        String correo = v.texto(email, "Email", true);
        v.exigir(password.getPassword().length > 0, password, "Contraseña");
        if (!v.ok()) return;
        String clave = new String(password.getPassword());
        ingresar.setEnabled(false);
        Tarea.ejecutar(this, () -> api.login(correo, clave), empleado -> {
            dispose();
            alIngresar.accept(empleado);
        }, error -> {
            // En el login, el 401 es "email o contraseña incorrectos": va al lado del formulario, no en un diálogo.
            // Sin conexión o un 500 no se arreglan cambiando la clave: esos van al diálogo de errores globales.
            ingresar.setEnabled(true);
            if (error.estado() != 401 && !error.esDelFormulario()) {
                Mensajes.error(this, error);
                return;
            }
            mensaje.setText(error.getMessage());
            password.selectAll();
            password.requestFocusInWindow();
        });
    }
}
