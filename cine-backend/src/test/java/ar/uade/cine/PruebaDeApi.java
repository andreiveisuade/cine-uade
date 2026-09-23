package ar.uade.cine;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.infrastructure.seguridad.Password;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * La API de verdad escuchando en un puerto libre, contra H2.
 *
 * <p>Existe porque hay una capa que los tests de gestor no pueden ver: el código HTTP. Un
 * gestor rechaza siempre igual, con {@code IllegalArgumentException}, y que eso salga como
 * 400 y no como 404 —o al revés— se decide en {@code controller/}. Esa traducción es parte
 * del contrato que firmamos con el frontend, así que se prueba pidiendo de verdad.
 *
 * <p>Levanta la aplicación entera y no una configuración armada para la prueba: registrar
 * los controladores a mano dejaría afuera justamente lo que se quiere comprobar —el
 * {@code @RestControllerAdvice} y el mapper de JSON— y el test seguiría en verde el día que
 * alguno de los dos cambie.
 *
 * <p>Por la misma razón pasa por Spring Security: cada pedido de {@link #get}, {@link #post}
 * y {@link #put} va autenticado como un administrador de prueba, así los tests de cada
 * controlador siguen probando su regla y no el permiso. Qué pasa sin credenciales o con
 * otro rol lo prueba {@code SeguridadTest} con {@link #pedirComo}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PruebaDeApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final String EMAIL_ADMIN = "admin@prueba.test";
    public static final String CLAVE_ADMIN = "clave-de-prueba";

    /**
     * Lejos de los ids que generan los tests: el usuario es una tabla sola para clientes y
     * empleados, y hay tests escritos sabiendo que el primer cliente que dan de alta es el
     * número uno. Guardarlo por el repositorio le robaría ese uno.
     */
    private static final int ID_ADMIN = 9000;

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    private BloqueoButacas bloqueoButacas;

    @Autowired
    private JdbcTemplate jdbc;

    /** El reloj del sistema bajo prueba: lo que los gestores leen como "ahora". */
    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        // El adaptador de bloqueos y el reloj son beans, o sea uno solo para toda la suite:
        // sin esto, la butaca que un test dejó elegida le aparece tomada al siguiente, y la
        // hora a la que otro movió el reloj le queda al que sigue.
        ((BloqueoButacasMemoria) bloqueoButacas).limpiar();
        reloj.reiniciar();
        jdbc.update("INSERT INTO usuario (id, nombre, email, rol, password_hash) VALUES (?, ?, ?, ?, ?)",
                ID_ADMIN, "Admin de prueba", EMAIL_ADMIN, "ADMINISTRADOR", Password.hashear(CLAVE_ADMIN));
    }

    protected Respuesta get(String ruta) {
        return pedirComo(HttpMethod.GET, ruta, null, EMAIL_ADMIN, CLAVE_ADMIN);
    }

    protected Respuesta post(String ruta, String cuerpo) {
        return pedirComo(HttpMethod.POST, ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN);
    }

    protected Respuesta put(String ruta, String cuerpo) {
        return pedirComo(HttpMethod.PUT, ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN);
    }

    /** Un pedido con las credenciales que se digan; con email {@code null}, sin ninguna. */
    protected Respuesta pedirComo(HttpMethod metodo, String ruta, String cuerpo,
                                  String email, String clave) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (cuerpo != null) {
            cabeceras.setContentType(MediaType.APPLICATION_JSON);
        }
        if (email != null) {
            cabeceras.setBasicAuth(email, clave);
        }
        return respuesta(cliente.exchange(URI.create(ruta), metodo,
                new HttpEntity<>(cuerpo, cabeceras), String.class));
    }

    private static Respuesta respuesta(ResponseEntity<String> entidad) {
        return new Respuesta(entidad.getStatusCode().value(), entidad.getBody(), entidad.getHeaders());
    }

    /** Lo que contestó el servidor, sin interpretar: el código, el cuerpo crudo y las cabeceras. */
    public record Respuesta(int estado, String cuerpo, HttpHeaders cabeceras) {

        public JsonNode json() {
            try {
                return JSON.readTree(cuerpo);
            } catch (Exception e) {
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
