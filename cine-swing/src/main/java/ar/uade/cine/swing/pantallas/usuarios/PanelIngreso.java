package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiSesion;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.Servidor.Entorno;
import ar.uade.cine.swing.api.dto.usuarios.Empleado;
import ar.uade.cine.swing.comun.Campos;
import ar.uade.cine.swing.comun.Colores;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Formulario;
import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Opcion;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.produccion.VentanaProduccion;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.prefs.Preferences;

// El formulario de ingreso con el servidor elegido; una clave mala se avisa junto al formulario, no en un diálogo.
// `conectar` arma un cliente por servidor: cambiar de servidor no arrastra las credenciales del otro.
public final class PanelIngreso extends JPanel {

    static final String CLAVE_SERVIDOR = "servidor";

    private final Preferences preferencias;
    private final Function<Servidor, ApiSesion> conectar;
    private final BiConsumer<Servidor, Empleado> alIngresar;
    private final JComboBox<Opcion<Entorno>> servidor = new JComboBox<>();
    private final JLabel etiquetaUrl = new JLabel("URL *");
    private final JTextField url = new JTextField(22);
    private final JTextField email = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JLabel mensaje = Componentes.texto(" ");
    private final JButton ingresar = Componentes.primario(new JButton("Ingresar"));
    private final JButton estado = new JButton("Estado de producción");

    public PanelIngreso(Preferences preferencias, Function<Servidor, ApiSesion> conectar, String aviso,
                        BiConsumer<Servidor, Empleado> alIngresar) {
        super(new BorderLayout());
        this.preferencias = preferencias;
        this.conectar = conectar;
        this.alIngresar = alIngresar;

        JLabel titulo = new JLabel("CINE UADE");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        mensaje.setForeground(Colores.error());
        if (aviso != null) mensaje.setText(aviso);
        Campos.llenar(servidor, Opcion.de(List.of(Entorno.values()), Entorno::etiqueta));

        add(new Formulario()
                .ancho(titulo)
                .ancho(Componentes.nota("Panel del encargado"))
                .campo("Servidor", servidor)
                .campo(etiquetaUrl, url)
                .obligatorio("Email", email)
                .obligatorio("Contraseña", password)
                .ancho(mensaje)
                .ancho(ingresar)
                .ancho(estado));

        mostrar(Servidor.configurado().orElseGet(() ->
                Servidor.de(preferencias.get(CLAVE_SERVIDOR, Servidor.LOCAL.url()))));
        servidor.addActionListener(e -> mostrarUrl());
        ingresar.addActionListener(e -> ingresar());
        estado.addActionListener(e -> VentanaProduccion.abrir(this, preferencias));
    }

    JButton botonIngresar() {
        return ingresar;
    }

    private void mostrar(Servidor elegido) {
        Campos.elegir(servidor, elegido.entorno());
        if (elegido.entorno() == Entorno.OTRO) url.setText(elegido.url());
        mostrarUrl();
    }

    private void mostrarUrl() {
        boolean otro = Campos.elegido(servidor) == Entorno.OTRO;
        etiquetaUrl.setVisible(otro);
        url.setVisible(otro);
        estado.setVisible(Campos.elegido(servidor) == Entorno.PRODUCCION);
        Window ventana = SwingUtilities.getWindowAncestor(this);
        if (ventana != null) ventana.pack();
    }

    private void ingresar() {
        Validacion v = new Validacion(mensaje);
        Entorno entorno = v.elegido(servidor, "Servidor");
        String otra = entorno == Entorno.OTRO ? v.url(url, "URL", true) : null;
        String correo = v.texto(email, "Email", true);
        v.exigir(password.getPassword().length > 0, password, "Contraseña");
        if (!v.ok()) return;
        Servidor elegido = otra != null ? Servidor.de(otra) : Servidor.de(entorno);
        preferencias.put(CLAVE_SERVIDOR, elegido.url());
        String clave = new String(password.getPassword());
        ApiSesion sesion = conectar.apply(elegido);
        if (elegido.duerme()) {
            mensaje.setForeground(Colores.aviso());
            mensaje.setText("Despertando el servidor de producción: puede tardar un minuto");
        }
        ingresar.setEnabled(false);
        Tarea.ejecutar(this, () -> sesion.login(correo, clave, elegido.esperaDelIngreso()),
                empleado -> alIngresar.accept(elegido, empleado), error -> {
            // En el login el 401 es clave incorrecta y va junto al formulario; sin conexión o un 500, al diálogo.
            ingresar.setEnabled(true);
            mensaje.setForeground(Colores.error());
            if (error.estado() != 401 && !error.esDelFormulario()) {
                mensaje.setText(" ");
                Mensajes.error(this, error);
                return;
            }
            mensaje.setText(error.getMessage());
            password.selectAll();
            password.requestFocusInWindow();
        });
    }
}
