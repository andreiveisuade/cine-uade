package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.Componentes;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

// Marca lo que todavía no se pasó a escritorio: mientras tanto se hace desde el panel web (admin.html).
final class PantallaPendiente extends JPanel {

    PantallaPendiente(String titulo) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(Componentes.encabezado(titulo, "Pendiente: esta pantalla todavía no está en el cliente de escritorio. "
                + "Mientras tanto se usa desde el panel web (admin.html)."), BorderLayout.NORTH);
    }
}
