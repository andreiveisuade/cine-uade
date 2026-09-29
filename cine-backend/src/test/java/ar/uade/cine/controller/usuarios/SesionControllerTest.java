package ar.uade.cine.controller.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.service.usuarios.GestorEmpleados;

class SesionControllerTest extends PruebaDeApi {

    private static final String CREDENCIALES_EN_EL_CUERPO = """
            {"email":"%s","password":"%s"}""".formatted(EMAIL_ADMIN, CLAVE_ADMIN);

    @Autowired
    private GestorEmpleados empleados;

    @BeforeEach
    void unAcomodador() {
        empleados.registrar("Portero", "puerta@cine.test", "clave-puerta", Rol.ACOMODADOR);
    }

    @Test
    @DisplayName("el administrador entra con Basic y recibe su empleado, sin el hash")
    void loginDelAdministrador() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("email").asText()).isEqualTo(EMAIL_ADMIN);
        assertThat(respuesta.json().get("rol").asText()).isEqualTo("ADMINISTRADOR");
        assertThat(respuesta.cuerpo()).doesNotContain("password");
    }

    @Test
    @DisplayName("el email del login no distingue mayúsculas, como en el resto de la API")
    void elEmailDelLoginNoDistingueMayusculas() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "Admin@PRUEBA.test", CLAVE_ADMIN);

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("email").asText()).isEqualTo(EMAIL_ADMIN);
    }

    @Test
    @DisplayName("el acomodador también entra, con su rol")
    void loginDelAcomodador() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "puerta@cine.test", "clave-puerta");

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("rol").asText()).isEqualTo("ACOMODADOR");
    }

    @Test
    @DisplayName("un hash SHA-256 de antes de bcrypt sigue entrando, y ese login lo pasa a bcrypt")
    void elHashViejoEntraYQuedaEnBcrypt() {
        assertThat(hashDe(EMAIL_ADMIN)).doesNotStartWith("{");

        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, CLAVE_ADMIN).estado())
                .isEqualTo(200);
        assertThat(hashDe(EMAIL_ADMIN)).startsWith("{bcrypt}");
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, CLAVE_ADMIN).estado())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("una clave equivocada no reescribe el hash")
    void unaClaveEquivocadaNoMigraElHash() {
        String antes = hashDe(EMAIL_ADMIN);

        pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, "otra-clave");

        assertThat(hashDe(EMAIL_ADMIN)).isEqualTo(antes);
    }

    @Test
    @DisplayName("una clave equivocada es 401 con el mismo mensaje que un email inexistente")
    void claveEquivocadaEs401() {
        Respuesta claveMala = pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, "otra-clave");
        Respuesta emailInexistente = pedirComo(HttpMethod.POST, "/api/sesion", null, "nadie@cine.test", CLAVE_ADMIN);

        assertThat(claveMala.estado()).isEqualTo(401);
        assertThat(claveMala.error()).isEqualTo("Email o contraseña incorrectos");
        assertThat(emailInexistente.estado()).isEqualTo(401);
        assertThat(emailInexistente.error()).isEqualTo(claveMala.error());
        assertThat(claveMala.cabeceras().containsKey("WWW-Authenticate")).isFalse();
    }

    @Test
    @DisplayName("sin header Basic es 401, aunque el cuerpo traiga las credenciales")
    void sinHeaderEs401AunqueVenganEnElCuerpo() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", CREDENCIALES_EN_EL_CUERPO, null, null);

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Hace falta iniciar sesión para esta operación");
    }

    @Test
    @DisplayName("un cliente viejo que manda header y además el cuerpo sigue entrando")
    void elCuerpoDeMasNoRompe() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", CREDENCIALES_EN_EL_CUERPO,
                EMAIL_ADMIN, CLAVE_ADMIN);

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("email").asText()).isEqualTo(EMAIL_ADMIN);
    }

    private String hashDe(String email) {
        return empleados.buscarPorEmail(email).orElseThrow().getPasswordHash();
    }
}
