package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Corre un pedido al backend fuera del EDT y vuelve al EDT con el resultado. Sin esto, un servidor lento congela la
 * ventana entera: Swing pinta y atiende clics en el mismo hilo.
 */
public final class Tarea {

    private Tarea() {
    }

    /** El error se muestra tal cual lo mandó el backend, en un diálogo sobre {@code origen}. */
    public static <T> void ejecutar(Component origen, Callable<T> trabajo, Consumer<T> alTerminar) {
        ejecutar(origen, trabajo, alTerminar, error -> {
            // El 401 ya lo atiende ApiHttp mandando al login, que dice por qué: un diálogo encima sería ruido.
            if (!error.esSesionVencida()) mostrarError(origen, error);
        });
    }

    public static <T> void ejecutar(Component origen, Callable<T> trabajo, Consumer<T> alTerminar,
                                    Consumer<ErrorApi> alFallar) {
        Cursor anterior = origen.getCursor();
        origen.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                origen.setCursor(anterior);
                T resultado;
                try {
                    resultado = get();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    ErrorApi error = causa instanceof ErrorApi api ? api
                            : new ErrorApi(0, causa.getMessage() != null ? causa.getMessage() : causa.toString());
                    alFallar.accept(error);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                alTerminar.accept(resultado);
            }
        }.execute();
    }

    public static void mostrarError(Component origen, ErrorApi error) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(origen), error.getMessage(), "No se pudo",
                JOptionPane.ERROR_MESSAGE);
    }
}
