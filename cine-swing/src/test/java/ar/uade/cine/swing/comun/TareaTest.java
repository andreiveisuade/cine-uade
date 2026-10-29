package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El cursor de espera con pedidos superpuestos sobre la misma pantalla, como las miniaturas de los pósters.
class TareaTest {

    @Test
    void elCursorVuelveCuandoTerminaElUltimoPedidoYNoElPrimero() throws Exception {
        JPanel pantalla = new JPanel();
        CountDownLatch soltarPrimero = new CountDownLatch(1);
        CountDownLatch soltarSegundo = new CountDownLatch(1);
        AtomicInteger terminados = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            Tarea.ejecutar(pantalla, () -> soltarPrimero.await(5, TimeUnit.SECONDS), r -> terminados.incrementAndGet());
            Tarea.ejecutar(pantalla, () -> soltarSegundo.await(5, TimeUnit.SECONDS), r -> terminados.incrementAndGet());
        });

        soltarPrimero.countDown();
        esperar(() -> terminados.get() == 1);
        SwingUtilities.invokeAndWait(() -> assertEquals(Cursor.WAIT_CURSOR, pantalla.getCursor().getType(),
                "queda un pedido en curso"));

        soltarSegundo.countDown();
        esperar(() -> terminados.get() == 2);
        SwingUtilities.invokeAndWait(() -> assertEquals(Cursor.DEFAULT_CURSOR, pantalla.getCursor().getType()));
    }

    private static void esperar(java.util.function.BooleanSupplier condicion) throws Exception {
        long limite = System.currentTimeMillis() + 5000;
        while (!condicion.getAsBoolean() && System.currentTimeMillis() < limite) Thread.sleep(10);
        assertTrue(condicion.getAsBoolean(), "no terminó a tiempo");
        SwingUtilities.invokeAndWait(() -> { });
    }
}
