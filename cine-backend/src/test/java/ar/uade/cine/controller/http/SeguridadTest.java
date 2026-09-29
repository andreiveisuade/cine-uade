package ar.uade.cine.controller.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorEmpleados;
import ar.uade.cine.service.ventas.GestorReservas;

class SeguridadTest extends PruebaDeApi {

    private static final String SALA = """
            {"nombre":"Sala 1","tipo":"DOS_D","butacasPorFila":[10,10]}""";

    @Autowired
    private GestorEmpleados empleados;

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

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

    // El mismo Content-Type que los errores de ManejadorErrores: un cliente no tiene por qué distinguirlos.
    @Test
    @DisplayName("un 401 o un 403 sale como application/json, igual que cualquier otro error, y con tildes")
    void losErroresDeSeguridadSalenComoLosDeLaApi() {
        Respuesta sinCredenciales = pedirComo(HttpMethod.POST, "/api/salas", SALA, null, null);
        Respuesta sinPermiso = pedirComo(HttpMethod.POST, "/api/salas", SALA, "puerta@cine.test", "clave-puerta");

        assertThat(sinCredenciales.cabeceras().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(sinPermiso.cabeceras().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(sinCredenciales.error()).isEqualTo("Hace falta iniciar sesión para esta operación");
    }

    @Test
    @DisplayName("el acomodador valida entradas: pasa el filtro y contesta el gestor")
    void acomodadorEnLaPuerta() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/acceso", "{\"codigo\":\"NOEXISTE\"}",
                "puerta@cine.test", "clave-puerta");

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

    @Test
    @DisplayName("la reserva por id es del encargado: el id se adivina")
    void reservaPorIdEsDelEncargado() {
        Reserva reserva = unaReserva();

        assertThat(pedirComo(HttpMethod.GET, "/api/reservas/" + reserva.getId(), null, null, null).estado())
                .isEqualTo(401);
        assertThat(pedirComo(HttpMethod.POST, "/api/reservas/" + reserva.getId() + "/cancelacion",
                null, null, null).estado()).isEqualTo(401);
        assertThat(get("/api/reservas/" + reserva.getId()).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("el cliente ve y cancela su reserva con el código, sin credenciales")
    void reservaPorCodigoEsPublica() {
        Reserva reserva = unaReserva();
        String ruta = "/api/reservas/codigo/" + reserva.getCodigo().toLowerCase();

        Respuesta detalle = pedirComo(HttpMethod.GET, ruta, null, null, null);
        assertThat(detalle.estado()).isEqualTo(200);
        assertThat(detalle.json().get("id").asInt()).isEqualTo(reserva.getId());

        Respuesta cancelada = pedirComo(HttpMethod.POST, ruta + "/cancelacion", null, null, null);
        assertThat(cancelada.estado()).isEqualTo(200);
        assertThat(cancelada.json().get("estado").asText()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("un código inexistente es 404, no 401")
    void codigoInexistenteEs404() {
        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/reservas/codigo/NOEXISTE", null, null, null);

        assertThat(respuesta.estado()).isEqualTo(404);
        assertThat(respuesta.error()).isEqualTo("No existe ninguna reserva con ese código");
    }

    @Test
    @DisplayName("mis reservas por email no revela el código de acceso")
    void misReservasSinCodigo() {
        unaReserva();

        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/reservas?email=andrei@uade.edu.ar",
                null, null, null);

        assertThat(respuesta.json()).hasSize(1);
        assertThat(respuesta.json().get(0).has("codigo")).isFalse();
        assertThat(get("/api/reservas").json().get(0).has("codigo")).isTrue();
    }

    // El firewall rechaza antes de cualquier controller y el pedido termina en /error: sin
    // ErroresController, el JSON de Boot decía "Bad Request", en inglés, y eso mostraba Swing.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            punto y coma en la ruta, /api/peliculas/1;x=1
            doble barra,             /api//cartelera
            punto codificado,        /api/salas/%2e%2e/1
            """)
    @DisplayName("lo que rechaza el firewall es 400 con {error} en castellano")
    void elRechazoDelFirewallSaleComoLosDeLaApi(String caso, String ruta) {
        Respuesta respuesta = pedirComo(HttpMethod.GET, ruta, null, null, null);

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.cabeceras().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(respuesta.error()).isEqualTo("El pedido no es válido");
    }

    private Reserva unaReserva() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        return reservas.reservar(funcion.getId(), "Andrei", "andrei@uade.edu.ar",
                Map.of("A1", TipoTarifa.GENERAL), null);
    }

    // Un header que no se puede leer no es un 500 ni abre el cuadro del navegador: 401 con {error}.
    // Basic roto dice lo mismo que una clave equivocada; otro esquema es como no mandar nada.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            base64 roto,               Basic !!!,                          Email o contraseña incorrectos
            sin los dos puntos,        Basic YWRtaW5AcHJ1ZWJhLnRlc3Q=,     Email o contraseña incorrectos
            Basic vacío,               Basic,                              Email o contraseña incorrectos
            email y clave vacíos,      Basic Og==,                         Email o contraseña incorrectos
            otro esquema,              Bearer x,                           Hace falta iniciar sesión para esta operación
            """)
    @DisplayName("un header Authorization mal formado es 401 en JSON")
    void unHeaderMalFormadoEs401EnJson(String caso, String autorizacion, String mensaje) {
        Respuesta respuesta = conAutorizacion(autorizacion);

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.cabeceras().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(respuesta.error()).isEqualTo(mensaje);
        assertThat(respuesta.cabeceras().containsKey("WWW-Authenticate")).isFalse();
    }

    @Test
    @DisplayName("el login no distingue los espacios alrededor del email")
    void elLoginIgnoraLosEspaciosAlrededorDelEmail() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "  " + EMAIL_ADMIN + " ", CLAVE_ADMIN);

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("email").asText()).isEqualTo(EMAIL_ADMIN);
    }

    // El admin tiene el hash SHA-256 viejo y el acomodador uno bcrypt: los dos caminos de verificación.
    @Test
    @DisplayName("una contraseña vacía es 401, con hash viejo o con bcrypt")
    void unaContrasenaVaciaEs401() {
        Respuesta admin = pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, "");
        Respuesta acomodador = pedirComo(HttpMethod.POST, "/api/sesion", null, "puerta@cine.test", "");

        assertThat(admin.estado()).isEqualTo(401);
        assertThat(admin.error()).isEqualTo("Email o contraseña incorrectos");
        assertThat(acomodador.estado()).isEqualTo(401);
        assertThat(acomodador.error()).isEqualTo("Email o contraseña incorrectos");
    }

    // bcrypt no admite más de 72 bytes: la comparación tiene que decir que no, sin tirar.
    @Test
    @DisplayName("una contraseña de más de 72 bytes contra un hash bcrypt es 401, no 500")
    void unaContrasenaDeMasDe72BytesEs401() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "puerta@cine.test", "x".repeat(100));

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Email o contraseña incorrectos");
    }

    // Con NULL en la columna, armar el usuario de Spring Security tiraba y el 401 decía otra cosa.
    @Test
    @DisplayName("un empleado sin hash en la base no entra, con el mismo 401 que una clave equivocada")
    void unEmpleadoSinHashEnLaBaseEs401() {
        jdbc.update("INSERT INTO usuario (nombre, email, rol, password_hash) VALUES (?, ?, ?, NULL)",
                "Sin clave", "sinclave@cine.test", "ADMINISTRADOR");

        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/sesion", null, "sinclave@cine.test", "cualquiera");

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Email o contraseña incorrectos");
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    private Respuesta conAutorizacion(String autorizacion) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.set(HttpHeaders.AUTHORIZATION, autorizacion);
        ResponseEntity<String> respuesta = http.exchange(URI.create("/api/sesion"), HttpMethod.POST,
                new HttpEntity<>(null, cabeceras), String.class);
        return new Respuesta(respuesta.getStatusCode().value(), respuesta.getBody(), respuesta.getHeaders());
    }
}
