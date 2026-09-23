package ar.uade.cine.controller.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.service.usuarios.GestorEmpleados;

/**
 * El resto de los tests de API va como administrador y no ve el filtro. Acá: el cliente sin
 * clave, la escritura del encargado rechazada con {@code {"error": "..."}} y sin la cabecera
 * que abre el login del navegador.
 */
class SeguridadTest extends PruebaDeApi {

    private static final String SALA = """
            {"nombre":"Sala 1","tipo":"DOS_D","butacasPorFila":[10,10]}""";

    @Autowired
    private GestorEmpleados empleados;

    @BeforeEach
    void unAcomodador() {
        empleados.registrar("Portero", "puerta@cine.test", "clave-puerta", Rol.ACOMODADOR);
    }

    @Test
    @DisplayName("la cartelera y la carta del candy se leen sin credenciales")
    void lecturaPublicaSinCredenciales() {
        assertThat(pedirComo(HttpMethod.GET, "/api/cartelera", null, null, null).estado())
                .isEqualTo(200);
        assertThat(pedirComo(HttpMethod.GET, "/api/candy/productos", null, null, null).estado())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("una escritura del encargado sin credenciales es 401 con {error} y sin WWW-Authenticate")
    void escrituraSinCredencialesEs401() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/salas", SALA, null, null);

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Hace falta iniciar sesión para esta operación");
        assertThat(respuesta.cabeceras().containsKey("WWW-Authenticate")).isFalse();
    }

    @Test
    @DisplayName("una contraseña equivocada es 401, con el mismo mensaje que el login")
    void claveEquivocadaEs401() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/salas", SALA, EMAIL_ADMIN, "otra-clave");

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Email o contraseña incorrectos");
        assertThat(respuesta.cabeceras().containsKey("WWW-Authenticate")).isFalse();
    }

    @Test
    @DisplayName("el acomodador no puede dar de alta una sala: 403 con {error}")
    void acomodadorEnRutaDeAdministradorEs403() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/salas", SALA, "puerta@cine.test", "clave-puerta");

        assertThat(respuesta.estado()).isEqualTo(403);
        assertThat(respuesta.error()).isEqualTo("Tu rol no tiene permiso para esta operación");
    }

    @Test
    @DisplayName("el acomodador valida entradas: pasa el filtro y contesta el gestor")
    void acomodadorEnLaPuerta() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/acceso", "{\"codigo\":\"NOEXISTE\"}",
                "puerta@cine.test", "clave-puerta");

        // Un código inventado lo rechaza la regla de negocio, no la seguridad.
        assertThat(respuesta.estado()).isNotIn(401, 403);
        assertThat(respuesta.json().has("error")).isTrue();
    }

    @Test
    @DisplayName("la puerta sin credenciales es 401")
    void puertaSinCredencialesEs401() {
        assertThat(pedirComo(HttpMethod.POST, "/api/acceso", "{\"codigo\":\"X\"}", null, null).estado())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("el cliente reserva sin credenciales: el pedido llega al gestor")
    void reservaDelClienteSinCredenciales() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/reservas", "{\"funcionId\":999}", null, null);

        assertThat(respuesta.estado()).isNotIn(401, 403);
    }

    @Test
    @DisplayName("mis reservas es público con email; el listado completo, del encargado")
    void listadoDeReservasSegunElEmail() {
        assertThat(pedirComo(HttpMethod.GET, "/api/reservas?email=alguien@mail.com", null, null, null).estado())
                .isEqualTo(200);
        assertThat(pedirComo(HttpMethod.GET, "/api/reservas", null, null, null).estado())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("el buzón de pendientes no queda abierto por parecerse a /api/peliculas/{id}")
    void pendientesEsDelEncargado() {
        assertThat(pedirComo(HttpMethod.GET, "/api/peliculas/pendientes", null, null, null).estado())
                .isEqualTo(401);
    }
}
