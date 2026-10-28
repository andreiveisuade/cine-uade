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

import static ar.uade.cine.swing.comun.Formato.escapar;

// Busca un cliente por email, para confirmar en boletería quién es; la cuenta del cliente es la de Google (R28).
public final class PantallaClientes extends Pantalla {

    private final ApiClientes apiClientes;
    private final JTextField email = new JTextField(24);
    private final JLabel error = Componentes.texto(" ");
    private final JLabel encontrado = Componentes.texto(" ");

    public PantallaClientes(ApiClientes apiClientes) {
        super("Clientes", "Buscá un cliente por email. Entra a la web con su cuenta de Google: no tiene clave del cine.");
        this.apiClientes = apiClientes;

        JButton buscar = new JButton("Buscar");
        JPanel fila = new JPanel(new BorderLayout(6, 0));
        fila.add(email, BorderLayout.CENTER);
        fila.add(buscar, BorderLayout.EAST);

        JPanel formulario = new JPanel();
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.add(Componentes.izquierda(new JLabel("Email *")));
        formulario.add(Componentes.izquierda(fila));
        formulario.add(Componentes.izquierda(error));
        formulario.add(Componentes.izquierda(encontrado));
        add(Componentes.lateral(formulario), BorderLayout.WEST);

        email.addActionListener(e -> buscar());
        buscar.addActionListener(e -> buscar());
    }

    private void buscar() {
        Validacion v = new Validacion(error);
        String correo = v.email(email, "Email", true);
        if (!v.ok()) return;
        mostrar(null);
        Tarea.ejecutar(this, () -> apiClientes.buscarClientePorEmail(correo), this::mostrar, v::mostrarError);
    }

    private void mostrar(Cliente buscado) {
        encontrado.setText(buscado == null ? " "
                : "<html><b>" + escapar(buscado.nombre()) + "</b><br>" + escapar(buscado.email()) + "</html>");
        revalidate();
    }
}
