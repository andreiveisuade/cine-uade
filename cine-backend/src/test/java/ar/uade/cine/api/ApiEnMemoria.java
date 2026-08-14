package ar.uade.cine.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.importador.ImportadorDePrueba;
import ar.uade.cine.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.BloqueoButacasMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ImportacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import io.javalin.Javalin;

/**
 * La API de verdad —{@link ServidorApi#armar}, con sus rutas y su traducción de errores—
 * escuchando en un puerto libre, contra la aplicación en memoria.
 *
 * <p>Existe porque hay una capa que los tests de gestor no pueden ver: el código HTTP. Un
 * gestor rechaza siempre igual, con {@code IllegalArgumentException}, y que eso salga como
 * 400 y no como 404 —o al revés— se decide en {@code api/}. Esa traducción es parte del
 * contrato que firmamos con el frontend, así que se prueba pidiendo de verdad.
 *
 * <p>Levanta el servidor entero y no solo la clase de rutas a propósito: registrar la ruta
 * a mano en un Javalin armado para la prueba dejaría afuera justamente lo que se quiere
 * comprobar —el mapeo de excepciones y el mapper de JSON— y el test seguiría en verde el
 * día que {@code ServidorApi} cambie alguno de los dos.
 *
 * <p>Puerto <code>0</code>: lo elige el sistema operativo. Fijar uno haría que dos tests en
 * paralelo, o un backend abierto en otra terminal, hicieran fallar la prueba por algo que
 * no tiene nada que ver con el código.
 */
public class ApiEnMemoria implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Aplicacion aplicacion;
    private final Javalin servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ImportadorDePrueba importador = new ImportadorDePrueba();

    /**
     * @param directorio dónde caen los comprobantes. Va por parámetro para que cada test le
     *                   pase su {@code @TempDir} y no se escriba en el repo al correr la suite
     */
    public ApiEnMemoria(Path directorio) {
        aplicacion = new Aplicacion(
                new PeliculaDAOMemoria(), new SalaDAOMemoria(), new AsientoDAOMemoria(),
                new FuncionDAOMemoria(), new ClienteDAOMemoria(), new EmpleadoDAOMemoria(),
                new ReservaDAOMemoria(), new PagoDAOMemoria(), new PromocionDAOMemoria(),
                new ProgramacionDAOMemoria(), new ProductoDAOMemoria(), new CompraCandyDAOMemoria(),
                new ImportacionDAOMemoria(), new BloqueoButacasMemoria(),
                new GeneradorTicketTxt(directorio.resolve("tickets")),
                new GeneradorTicketCandyTxt(directorio.resolve("tickets")),
                new GeneradorReciboTxt(directorio.resolve("tickets")),
                new GeneradorBorderoTxt(directorio.resolve("informes")),
                new MercadoPagoEmulado(), importador);
        servidor = ServidorApi.armar(aplicacion).start(0);
    }

    /** Los gestores de esta misma API: es con lo que el test arma el escenario. */
    public Aplicacion aplicacion() {
        return aplicacion;
    }

    /** El importador que atiende esta API, para decirle qué tiene que contestar. */
    public ImportadorDePrueba importador() {
        return importador;
    }

    public Respuesta get(String ruta) {
        return pedir(HttpRequest.newBuilder(uri(ruta)).GET());
    }

    public Respuesta post(String ruta, String cuerpo) {
        return pedir(HttpRequest.newBuilder(uri(ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)));
    }

    @Override
    public void close() {
        servidor.stop();
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + servidor.port() + ruta);
    }

    private Respuesta pedir(HttpRequest.Builder pedido) {
        try {
            HttpResponse<String> respuesta = cliente.send(pedido.build(),
                    HttpResponse.BodyHandlers.ofString());
            return new Respuesta(respuesta.statusCode(), respuesta.body());
        } catch (IOException e) {
            throw new IllegalStateException("Falló el pedido a la API de prueba", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió el pedido a la API de prueba", e);
        }
    }

    /** Lo que contestó el servidor, sin interpretar: el código y el cuerpo crudo. */
    public record Respuesta(int estado, String cuerpo) {

        /** El JSON ya parseado, para no afirmar cosas sobre un {@code String} con llaves. */
        public JsonNode json() {
            try {
                return JSON.readTree(cuerpo);
            } catch (IOException e) {
                throw new IllegalStateException("La respuesta no era JSON: " + cuerpo, e);
            }
        }

        /**
         * El mensaje de error tal como lo va a mostrar el front. Se afirma sobre el texto
         * completo y no sobre un pedazo: el contrato dice que sale <em>intacto</em> desde el
         * gestor, y un {@code contains} dejaría pasar que la capa HTTP lo reescriba.
         */
        public String error() {
            return json().get("error").asText();
        }
    }
}
