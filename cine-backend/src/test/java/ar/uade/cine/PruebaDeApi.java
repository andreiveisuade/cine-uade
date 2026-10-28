package ar.uade.cine;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.infrastructure.seguridad.ClientesDeGoogle;
import ar.uade.cine.infrastructure.seguridad.LimiteDeIntentos;
import ar.uade.cine.infrastructure.seguridad.ConfiguracionSeguridad;
import ar.uade.cine.infrastructure.seguridad.PedidosDeGoogle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class PruebaDeApi {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static final String EMAIL_ADMIN = "admin@prueba.test";
    public static final String CLAVE_ADMIN = "clave-de-prueba";

    // Fijo y alto: los tests asumen que el primer cliente dado de alta tiene id 1.
    private static final int ID_ADMIN = 9000;

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private LimpiezaDeBase limpieza;

    @Autowired
    private CatalogoDePrueba catalogoExterno;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    protected ConfiguracionDePrueba.RelojMovible reloj;

    @Autowired
    private LimiteDeIntentos intentos;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClientesDeGoogle google;

    @BeforeEach
    void dejarLaBaseComoNueva() {
        // Todos los pedidos salen de 127.0.0.1: los fallos de un test bloquearían la IP en los siguientes.
        intentos.olvidar();
        limpieza.limpiar();
        catalogoExterno.reiniciar();
        reloj.reiniciar();
        jdbc.update("INSERT INTO usuario (id, nombre, email, rol, password_hash) VALUES (?, ?, ?, ?, ?)",
                ID_ADMIN, "Admin de prueba", EMAIL_ADMIN, "ADMINISTRADOR", new ConfiguracionSeguridad().passwordEncoder().encode(CLAVE_ADMIN));
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

    protected Respuesta patch(String ruta, String cuerpo) {
        return pedirComo(HttpMethod.PATCH, ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN);
    }

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

    // R28 sin salir a Google: ClientesDeGoogle arma al usuario y oidcLogin() lo deja en la sesión, como el login real.
    protected MockHttpSession entrarConGoogle(String email) {
        MvcResult login = hacer(MockMvcRequestBuilders.post("/api/sesion").with(oidcLogin().oidcUser(
                google.loadUser(PedidosDeGoogle.de(email, "Cliente de Google", true)))));
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    protected Respuesta conSesion(HttpMethod metodo, String ruta, MockHttpSession sesion, String cuerpo) {
        MockHttpServletRequestBuilder pedido = MockMvcRequestBuilders.request(metodo, ruta).session(sesion);
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        MockHttpServletResponse respuesta = hacer(pedido).getResponse();
        HttpHeaders cabeceras = new HttpHeaders();
        respuesta.getHeaderNames().forEach(nombre -> cabeceras.addAll(nombre, respuesta.getHeaders(nombre)));
        try {
            return new Respuesta(respuesta.getStatus(), respuesta.getContentAsString(), cabeceras);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected MvcResult hacer(RequestBuilder pedido) {
        try {
            return mvc.perform(pedido).andReturn();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

        public String error() {
            return json().get("error").asText();
        }
    }
}
