package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.ErrorApi;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// Corre un pedido al backend fuera del EDT y vuelve con el resultado, para que la ventana no se congele.
public final class Tarea {

    // Varios pedidos a la vez sobre la misma pantalla (las miniaturas de los pósters) comparten la espera:
    // el cursor vuelve cuando termina el último, no el primero. Se usan solo desde el EDT.
    private static final Map<Component, Integer> EN_CURSO = new WeakHashMap<>();
    private static final Map<Component, Cursor> ANTERIOR = new WeakHashMap<>();

    private Tarea() {
    }

    /** Sin formulario al que atarlo, el error es global: va en un diálogo, ver {@link Mensajes#error}. */
    public static <T> void ejecutar(Component origen, Callable<T> trabajo, Consumer<T> alTerminar) {
        ejecutar(origen, trabajo, alTerminar, error -> Mensajes.error(origen, error));
    }

    public static <T> void ejecutar(Component origen, Callable<T> trabajo, Consumer<T> alTerminar,
                                    Consumer<ErrorApi> alFallar) {
        if (EN_CURSO.merge(origen, 1, Integer::sum) == 1) {
            ANTERIOR.put(origen, origen.getCursor());
            origen.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                if (EN_CURSO.merge(origen, -1, Integer::sum) == 0) {
                    EN_CURSO.remove(origen);
                    origen.setCursor(ANTERIOR.remove(origen));
                }
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
