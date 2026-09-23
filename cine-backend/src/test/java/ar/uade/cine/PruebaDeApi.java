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
 * La API real en un puerto libre contra H2, para probar la traducción a HTTP (códigos,
 * {@code @RestControllerAdvice}, JSON) que es contrato con el front. {@link #get},
 * {@link #post} y {@link #put} van como administrador; los permisos los prueba
 * {@code SeguridadTest} con {@link #pedirComo}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class PruebaDeApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final String EMAIL_ADMIN = "admin@prueba.test";
    public static final String CLAVE_ADMIN = "clave-de-prueba";

    /** Fijo y alto: los tests asumen que el primer cliente dado de alta tiene id 1. */
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

    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        // Bloqueos y reloj son beans compartidos por toda la suite.
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

    public record Respuesta(int estado, String cuerpo, HttpHeaders cabeceras) {

        public JsonNode json() {
            try {
                return JSON.readTree(cuerpo);
            } catch (Exception e) {
                throw new IllegalStateException("La respuesta no era JSON: " + cuerpo, e);
            }
        }

        /** Se compara entero: el contrato dice que el mensaje del gestor llega intacto. */
        public String error() {
            return json().get("error").asText();
        }
    }
}
