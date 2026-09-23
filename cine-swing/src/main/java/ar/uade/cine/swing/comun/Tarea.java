package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

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

    /** Sin formulario al que atarlo, el error es global: va en un diálogo, ver {@link Mensajes#error}. */
    public static <T> void ejecutar(Component origen, Callable<T> trabajo, Consumer<T> alTerminar) {
        ejecutar(origen, trabajo, alTerminar, error -> Mensajes.error(origen, error));
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
                            : new ErrorApi(-1, causa.getMessage() != null ? causa.getMessage() : causa.toString());
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
}
