package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.api.ApiHttp;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Lo común a toda pantalla del panel: margen, encabezado y los pedidos fuera del EDT con su aviso. */
abstract class Pantalla extends JPanel {

    protected final ApiHttp api;

    Pantalla(ApiHttp api, String titulo, String descripcion) {
        super(new BorderLayout(12, 12));
        this.api = api;
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(Componentes.encabezado(titulo, descripcion), BorderLayout.NORTH);
    }

    protected <T> void cargar(Callable<T> trabajo, Consumer<T> alTerminar) {
        Tarea.ejecutar(this, trabajo, alTerminar);
    }

    /** Una acción que escribe: si sale bien avisa con `mensaje` y sigue con `despues` (casi siempre, recargar). */
    protected <T> void accion(Callable<T> trabajo, String mensaje, Runnable despues) {
        Tarea.ejecutar(this, trabajo, resultado -> {
            if (mensaje != null) avisar(mensaje);
            if (despues != null) despues.run();
        });
    }

    protected void avisar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(this, pregunta, "Confirmar", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION;
    }
}
