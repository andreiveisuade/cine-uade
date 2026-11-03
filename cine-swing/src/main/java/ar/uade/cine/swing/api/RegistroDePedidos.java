package ar.uade.cine.swing.api;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Qué pedidos salieron a la API y cómo terminaron; Patrón Observer: el transporte no conoce a quien lo muestra.
// Guarda solo metadatos: ni credenciales ni cuerpos, que pueden traer datos de clientes.
public final class RegistroDePedidos {

    private static final int MAXIMO = 200;

    /** {@code estado}: el código HTTP o lo que pasó («sin red», «tardó»); {@code mensaje}: el error, si hubo. */
    public record Resultado(String estado, long recibidos, String tipo, String mensaje) {

        public static Resultado sinRespuesta(String estado, String mensaje) {
            return new Resultado(estado, 0, null, mensaje);
        }
    }

    /** {@code resultado} es null mientras carga. */
    public record Pedido(long id, LocalTime hora, String metodo, String ruta, String url, long enviados, long desde,
                         Resultado resultado, long ms) {

        public boolean enCurso() {
            return resultado == null;
        }

        public boolean fallo() {
            return !enCurso() && !resultado.estado().startsWith("2") && !resultado.estado().startsWith("3");
        }

        public long duracionMs() {
            return enCurso() ? (System.nanoTime() - desde) / 1_000_000 : ms;
        }
    }

    private final Deque<Pedido> pedidos = new ArrayDeque<>();
    private final List<Runnable> oyentes = new CopyOnWriteArrayList<>();
    private long ultimoId;

    /** Avisa desde el hilo del pedido: quien lo registra salta al EDT. */
    public void alCambiar(Runnable oyente) {
        oyentes.add(oyente);
    }

    public Pedido empezar(String metodo, String ruta, String url, long enviados) {
        Pedido pedido;
        synchronized (this) {
            pedido = new Pedido(++ultimoId, LocalTime.now(), metodo, sinDatos(ruta), sinDatos(url), enviados,
                    System.nanoTime(), null, 0);
            pedidos.addFirst(pedido);
            if (pedidos.size() > MAXIMO) pedidos.removeLast();
        }
        avisar();
        return pedido;
    }

    public Pedido terminar(Pedido pedido, Resultado resultado) {
        Pedido terminado = new Pedido(pedido.id(), pedido.hora(), pedido.metodo(), pedido.ruta(), pedido.url(),
                pedido.enviados(), pedido.desde(), resultado, (System.nanoTime() - pedido.desde()) / 1_000_000);
        synchronized (this) {
            List<Pedido> actuales = pedidos.stream().map(p -> p.id() == pedido.id() ? terminado : p).toList();
            pedidos.clear();
            pedidos.addAll(actuales);
        }
        avisar();
        return terminado;
    }

    /** Del más nuevo al más viejo, hasta los últimos {@value #MAXIMO}. */
    public synchronized List<Pedido> recientes() {
        return List.copyOf(pedidos);
    }

    // Los valores de la query no se guardan: ahí viaja, por ejemplo, el email de un cliente.
    private static String sinDatos(String ruta) {
        return ruta.replaceAll("=[^&]*", "=…");
    }

    private void avisar() {
        oyentes.forEach(Runnable::run);
    }
}
