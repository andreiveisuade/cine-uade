package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.Mensajes;
import ar.uade.cine.swing.comun.Tarea;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

// Una parte de pantalla (una pestaña, un panel) con los atajos de siempre para pedir fuera del EDT y avisar.
public abstract class Seccion extends JPanel {

    protected Seccion(LayoutManager layout) {
        super(layout);
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

    /** Lo mismo para lo que no devuelve nada, como un borrado. */
    protected void accion(Runnable trabajo, String mensaje, Runnable despues) {
        accion(() -> {
            trabajo.run();
            return null;
        }, mensaje, despues);
    }

    /** Lo que salió bien, en la barra de estado: ver {@link Mensajes}. */
    protected void avisar(String mensaje) {
        Mensajes.exito(this, mensaje);
    }

    /** {@code si}: el botón que confirma, con el verbo de la acción ("Sí, borrar"). */
    protected boolean confirmar(String pregunta, String si) {
        return Mensajes.confirmar(this, pregunta, si);
    }
}
