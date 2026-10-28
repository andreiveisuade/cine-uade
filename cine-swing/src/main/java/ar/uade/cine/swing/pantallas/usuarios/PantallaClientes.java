package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tarea;
import ar.uade.cine.swing.comun.Validacion;
import ar.uade.cine.swing.pantallas.Pantalla;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import static ar.uade.cine.swing.comun.Formato.escapar;

// Busca un cliente por email y le asigna una clave temporal (R27), para quien perdió la clave y los tickets.
// La clave se ve una sola vez, acá: no se guarda, y buscar otro cliente o salir de la pantalla la borra.
public final class PantallaClientes extends Pantalla {

    private final ApiClientes apiClientes;
    private final JTextField email = new JTextField(24);
    private final JLabel error = Componentes.texto(" ");
    private final JLabel encontrado = Componentes.texto(" ");
    private final JButton asignar = new JButton("Asignar clave temporal");
    private final JTextField clave = new JTextField(14);
    private final JButton copiar = new JButton("Copiar");
    private Cliente cliente;

    public PantallaClientes(ApiClientes apiClientes) {
        super("Clientes", "Buscá un cliente por email y asignale una clave temporal: la web le pide cambiarla al entrar.");
        this.apiClientes = apiClientes;

        JButton buscar = new JButton("Buscar");
        JPanel fila = new JPanel(new BorderLayout(6, 0));
        fila.add(email, BorderLayout.CENTER);
        fila.add(buscar, BorderLayout.EAST);
        clave.setEditable(false);
        clave.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));

        JPanel formulario = new JPanel();
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.add(Componentes.izquierda(new JLabel("Email *")));
        formulario.add(Componentes.izquierda(fila));
        formulario.add(Componentes.izquierda(error));
        formulario.add(Componentes.izquierda(encontrado));
        formulario.add(Componentes.izquierda(asignar));
        formulario.add(Componentes.izquierda(Componentes.botones(copiar)));
        formulario.add(Componentes.izquierda(clave));
        add(Componentes.lateral(formulario), BorderLayout.WEST);

        mostrar(null);
        email.addActionListener(e -> buscar());
        buscar.addActionListener(e -> buscar());
        asignar.addActionListener(e -> asignar());
        copiar.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(clave.getText()), null));
    }

    private void buscar() {
        Validacion v = new Validacion(error);
        String correo = v.email(email, "Email", true);
        if (!v.ok()) return;
        mostrar(null);
        Tarea.ejecutar(this, () -> apiClientes.buscarClientePorEmail(correo), this::mostrar, v::mostrarError);
    }

    private void asignar() {
        if (!confirmar("¿Asignarle una clave temporal a " + cliente.nombre() + "? La clave que tenga deja de valer.",
                "Sí, asignar")) return;
        Tarea.ejecutar(this, () -> apiClientes.asignarClaveTemporal(cliente.id()), temporal -> {
            clave.setText(temporal.clave());
            clave.setVisible(true);
            copiar.setVisible(true);
            clave.selectAll();
            clave.requestFocusInWindow();
            avisar("Clave temporal asignada a " + cliente.email() + ": pasásela, no se vuelve a mostrar");
        });
    }

    private void mostrar(Cliente buscado) {
        cliente = buscado;
        encontrado.setText(buscado == null ? " "
                : "<html><b>" + escapar(buscado.nombre()) + "</b><br>" + escapar(buscado.email()) + "</html>");
        asignar.setVisible(buscado != null);
        clave.setText("");
        clave.setVisible(false);
        copiar.setVisible(false);
        revalidate();
    }
}
