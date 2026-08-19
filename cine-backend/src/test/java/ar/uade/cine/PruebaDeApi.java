package ar.uade.cine;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import ar.uade.cine.infrastructure.bloqueos.BloqueoButacas;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

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
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PruebaDeApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    private BloqueoButacas bloqueoButacas;

    @Autowired
    private ConfiguracionDePrueba.Reloj relojDeLosBloqueos;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        // El adaptador de bloqueos y su reloj son beans, o sea uno solo para toda la suite:
        // sin esto, la butaca que un test dejó elegida le aparece tomada al siguiente.
        ((BloqueoButacasMemoria) bloqueoButacas).limpiar();
        relojDeLosBloqueos.reiniciar();
    }

    protected Respuesta get(String ruta) {
        return respuesta(cliente.getForEntity(ruta, String.class));
    }

    protected Respuesta post(String ruta, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        return respuesta(cliente.exchange(URI.create(ruta), HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras), String.class));
    }

    private static Respuesta respuesta(ResponseEntity<String> entidad) {
        return new Respuesta(entidad.getStatusCode().value(), entidad.getBody());
    }

    /** Lo que contestó el servidor, sin interpretar: el código y el cuerpo crudo. */
    public record Respuesta(int estado, String cuerpo) {

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
