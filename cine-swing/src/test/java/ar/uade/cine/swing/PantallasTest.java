package ar.uade.cine.swing;

import ar.uade.cine.swing.api.Apis;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.pantallas.Destino;
import ar.uade.cine.swing.pantallas.Navegacion;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cada entrada del menú y cada detalle se arman sin backend, en headless. Lo que se cuida es lo que mueve un refactor
 * de pantallas: el orden en que se inicializan los campos, un panel extraído que llega null, un callback que rompe. El
 * servidor falso contesta 401 a todo, como una sesión vencida: es la única respuesta de error que no abre un diálogo,
 * que en headless no se puede mostrar.
 */
class PantallasTest {

    private static final Navegacion QUIETA = new Navegacion() {
        @Override
        public void ir(Destino destino) {
        }

        @Override
        public void abrirInforme(int funcionId) {
        }

        @Override
        public void abrirCobro(int reservaId) {
        }

        @Override
        public void abrirMapa(int salaId) {
        }
    };

    private HttpServer servidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private final List<Throwable> errores = new ArrayList<>();
    private Thread.UncaughtExceptionHandler anterior;
    private Pantallas pantallas;

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            byte[] cuerpo = "{\"error\":\"Hace falta iniciar sesión para esta operación\"}"
                    .getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(401, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        // Lo que tira un callback en el EDT no llega al test: se junta acá y se revisa al final.
        anterior = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            synchronized (errores) {
                errores.add(error);
            }
        });
        ClienteHttp http = new ClienteHttp("http://127.0.0.1:" + servidor.getAddress().getPort());
        pantallas = new Pantallas(Apis.sobre(http), QUIETA);
    }

    @AfterEach
    void bajar() {
        Thread.setDefaultUncaughtExceptionHandler(anterior);
        servidor.stop(0);
    }

    @Test
    void cadaEntradaDelMenuSeArmaYAguantaUnaSesionVencida() throws Exception {
        for (Destino destino : Destino.values()) armar(destino.titulo(), () -> pantallas.de(destino));

        esperarLosPedidos();

        assertTrue(pedidos.get() > 0, "las pantallas piden sus datos al entrar");
        assertEquals(List.of(), errores);
    }

    @Test
    void cadaDetalleSeArmaYAguantaUnaSesionVencida() throws Exception {
        armar("borderó e informe", () -> pantallas.informe(1));
        armar("cobro", () -> pantallas.cobro(1));
        armar("mapa de sala", () -> pantallas.mapa(1));

        esperarLosPedidos();

        assertEquals(List.of(), errores);
    }

    private void armar(String nombre, Supplier<JComponent> pantalla) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JComponent armada = pantalla.get();
            armada.setSize(1200, 800);
            armada.doLayout();
            assertTrue(armada.getPreferredSize().width > 0, nombre + " no tiene nada adentro");
        });
    }

    // Los pedidos corren en SwingWorker: se espera a que dejen de llegar y a que el EDT corra lo que devolvieron.
    private void esperarLosPedidos() throws Exception {
        int vistos = -1;
        for (int intento = 0; intento < 50 && vistos != pedidos.get(); intento++) {
            vistos = pedidos.get();
            Thread.sleep(200);
        }
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> { });
    }
}
