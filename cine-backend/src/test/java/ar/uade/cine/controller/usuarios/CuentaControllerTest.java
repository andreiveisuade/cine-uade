package ar.uade.cine.controller.usuarios;

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

import ar.uade.cine.ConfiguracionDePrueba.RelojMovible;
import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.GestorReservas;

class CuentaControllerTest extends PruebaDeApi {

    private static final String ANA = "ana@mail.com";
    private static final String CLAVE = "clave-de-ana";
    private static final String NUEVA = "otra-clave-de-ana";
    private static final String NEUTRO =
            "El email y el código no corresponden a ninguna cuenta: revisá el ticket de una de tus compras";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

    private int funcionId;

    @BeforeEach
    void unaFuncionYAnaConCuenta() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        funcionId = funcion.getId();
        pedirComo(HttpMethod.POST, "/api/clientes",
                "{\"nombre\":\"Ana\",\"email\":\"" + ANA + "\",\"password\":\"" + CLAVE + "\"}", null, null);
    }

    @Test
    @DisplayName("R23: el cliente cambia su nombre con la sesión; el email queda")
    void cambiaSuNombre() {
        Respuesta respuesta = conCookie(HttpMethod.PUT, "/api/cuenta", entrar(ANA, CLAVE),
                "{\"nombre\":\"  Ana Pérez \",\"email\":\"otra@mail.com\"}");

        assertThat(respuesta.estado()).isEqualTo(200);
        assertThat(respuesta.json().get("nombre").asText()).isEqualTo("Ana Pérez");
        assertThat(respuesta.json().get("email").asText()).isEqualTo(ANA);
    }

    @ParameterizedTest(name = "R23: {0}")
    @CsvSource(textBlock = """
            sin nombre,        '""',    Falta el nombre
            nombre en blanco,  '"   "', Falta el nombre
            """)
    void elNombreNuevoPasaLasReglasDelAlta(String caso, String nombre, String error) {
        Respuesta respuesta = conCookie(HttpMethod.PUT, "/api/cuenta", entrar(ANA, CLAVE),
                "{\"nombre\":" + nombre + "}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).isEqualTo(error);
    }

    @Test
    @DisplayName("R23: un nombre de más de 100 caracteres es 400, como en el alta")
    void nombreLargo() {
        Respuesta respuesta = conCookie(HttpMethod.PUT, "/api/cuenta", entrar(ANA, CLAVE),
                "{\"nombre\":\"" + "a".repeat(101) + "\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).startsWith("El nombre");
    }

    @ParameterizedTest(name = "sin sesión: {0} {1} es 401")
    @CsvSource(textBlock = """
            PUT,    /api/cuenta,       '{"nombre":"Ana"}'
            POST,   /api/cuenta/clave, '{"actual":"clave-de-ana","nueva":"otra-clave"}'
            DELETE, /api/cuenta,       '{"actual":"clave-de-ana"}'
            """)
    void sinSesionEs401(HttpMethod metodo, String ruta, String cuerpo) {
        assertThat(pedirComo(metodo, ruta, cuerpo, null, null).estado()).isEqualTo(401);
    }

    @Test
    @DisplayName("R24: con la actual cambia la clave, la vieja deja de entrar y la sesión sigue con otro id")
    void cambiaLaClave() {
        String cookie = entrar(ANA, CLAVE);

        Respuesta cambio = conCookie(HttpMethod.POST, "/api/cuenta/clave", cookie,
                "{\"actual\":\"" + CLAVE + "\",\"nueva\":\"" + NUEVA + "\"}");

        assertThat(cambio.estado()).isEqualTo(204);
        String nueva = cookieDe(cambio);
        assertThat(nueva).isNotEqualTo(cookie);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", nueva).estado()).isEqualTo(200);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE).estado()).isEqualTo(401);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, NUEVA).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("R24: cambiar la clave cierra las otras sesiones del cliente")
    void cambiarLaClaveCierraLasOtrasSesiones() {
        String otroDispositivo = entrar(ANA, CLAVE);

        conCookie(HttpMethod.POST, "/api/cuenta/clave", entrar(ANA, CLAVE),
                "{\"actual\":\"" + CLAVE + "\",\"nueva\":\"" + NUEVA + "\"}");

        Respuesta vieja = conCookie(HttpMethod.GET, "/api/reservas/mias", otroDispositivo);
        assertThat(vieja.estado()).isEqualTo(401);
        assertThat(vieja.error()).isEqualTo("Hace falta iniciar sesión para esta operación");
    }

    @ParameterizedTest(name = "R24: {0}")
    @CsvSource(textBlock = """
            actual equivocada,  '{"actual":"no-es-esta","nueva":"otra-clave-de-ana"}', La contraseña actual no es correcta
            sin la actual,      '{"nueva":"otra-clave-de-ana"}',                       Falta la contraseña actual
            nueva corta,        '{"actual":"clave-de-ana","nueva":"corta"}',            La contraseña tiene que tener al menos 6 caracteres
            sin la nueva,       '{"actual":"clave-de-ana"}',                           Falta la contraseña
            """)
    void elCambioDeClaveSeRechaza(String caso, String cuerpo, String error) {
        Respuesta respuesta = conCookie(HttpMethod.POST, "/api/cuenta/clave", entrar(ANA, CLAVE), cuerpo);

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).isEqualTo(error);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE).estado()).isEqualTo(200);
    }

    @ParameterizedTest(name = "R24 y R26: cinco claves actuales equivocadas en {0} bloquean un minuto")
    @CsvSource(textBlock = """
            cambio de clave, POST,   /api/cuenta/clave, '{"actual":"%s","nueva":"otra-clave-de-ana"}'
            baja,            DELETE, /api/cuenta,       '{"actual":"%s"}'
            """)
    void laClaveActualPasaPorElLimiteDeIntentos(String caso, HttpMethod metodo, String ruta, String cuerpo) {
        String cookie = entrar(ANA, CLAVE);
        for (int i = 0; i < 5; i++) {
            assertThat(conCookie(metodo, ruta, cookie, cuerpo.formatted("no-es-esta")).estado()).isEqualTo(400);
        }

        Respuesta bloqueada = conCookie(metodo, ruta, cookie, cuerpo.formatted(CLAVE));

        assertThat(bloqueada.estado()).isEqualTo(429);
        assertThat(bloqueada.error()).isEqualTo("Demasiados intentos: esperá un minuto y probá de nuevo");
    }

    // Con el email completo, quien vea el ticket tendría las dos mitades de la recuperación.
    @Test
    @DisplayName("R25: el ticket público por código muestra el email enmascarado; el encargado lo ve entero")
    void elTicketPublicoEnmascaraElEmail() {
        Reserva suya = reservar(ANA, "A1");

        Respuesta publica = pedirComo(HttpMethod.GET, "/api/reservas/codigo/" + suya.getCodigo(), null, null, null);

        assertThat(publica.json().get("cliente").get("email").asText()).isEqualTo("a***@mail.com");
        assertThat(publica.cuerpo()).doesNotContain(ANA);
        assertThat(get("/api/reservas/" + suya.getId()).json().get("cliente").get("email").asText()).isEqualTo(ANA);
    }

    // Swing manda Basic sin sesión: la cuenta es solo del cliente.
    @ParameterizedTest(name = "el empleado no usa /api/cuenta: {0} {1} es 403")
    @CsvSource(textBlock = """
            PUT,    /api/cuenta,       '{"nombre":"Otro"}'
            POST,   /api/cuenta/clave, '{"actual":"clave-de-prueba","nueva":"otra-clave"}'
            DELETE, /api/cuenta,       '{"actual":"clave-de-prueba"}'
            """)
    void elEmpleadoNoTieneCuenta(HttpMethod metodo, String ruta, String cuerpo) {
        assertThat(pedirComo(metodo, ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN).estado()).isEqualTo(403);
    }

    @Test
    @DisplayName("R25: con el código de una entrada suya recupera la clave y cierra las sesiones de antes")
    void recuperaLaClave() {
        Reserva suya = reservar(ANA, "A1");
        String sesionDeAntes = entrar(ANA, CLAVE);

        Respuesta respuesta = recuperar(" ANA@mail.com ", suya.getCodigo().toLowerCase(), NUEVA);

        assertThat(respuesta.estado()).isEqualTo(204);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, NUEVA).estado()).isEqualTo(200);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE).estado()).isEqualTo(401);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", sesionDeAntes).estado()).isEqualTo(401);
    }

    // Distinguir los casos le diría a cualquiera qué emails tienen cuenta en el cine.
    @ParameterizedTest(name = "R25: {0}")
    @CsvSource(textBlock = """
            email inexistente,         nadie@mail.com, PROPIO
            email sin cuenta,          beto@mail.com,  DE_BETO
            código de otro cliente,    ana@mail.com,   DE_BETO
            código inventado,          ana@mail.com,   ZZZZZZZZ
            """)
    void sinPruebaDaElMismoMensajeNeutro(String caso, String email, String codigo) {
        Reserva propia = reservar(ANA, "A1");
        Reserva deBeto = reservar("beto@mail.com", "A2");
        String enviado = switch (codigo) {
            case "PROPIO" -> propia.getCodigo();
            case "DE_BETO" -> deBeto.getCodigo();
            default -> codigo;
        };

        Respuesta respuesta = recuperar(email, enviado, NUEVA);

        assertThat(respuesta.estado()).isEqualTo(403);
        assertThat(respuesta.error()).isEqualTo(NEUTRO);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("R25: cinco códigos equivocados bloquean el email un minuto, aunque el sexto sea el bueno")
    void laRecuperacionPasaPorElLimiteDeIntentos() {
        Reserva suya = reservar(ANA, "A1");
        for (int i = 0; i < 5; i++) {
            assertThat(recuperar(ANA, "ZZZZZZZZ", NUEVA).estado()).isEqualTo(403);
        }

        Respuesta bloqueada = recuperar(ANA, suya.getCodigo(), NUEVA);

        assertThat(bloqueada.estado()).isEqualTo(429);
        assertThat(bloqueada.error()).isEqualTo("Demasiados intentos: esperá un minuto y probá de nuevo");
        reloj.mover(RelojMovible.INICIO.plusMinutes(1));
        assertThat(recuperar(ANA, suya.getCodigo(), NUEVA).estado()).isEqualTo(204);
    }

    @Test
    @DisplayName("R25: la IP también se bloquea, aunque cada intento sea con otro email")
    void laRecuperacionCuentaPorIp() {
        Reserva suya = reservar(ANA, "A1");
        for (int i = 0; i < 5; i++) {
            recuperar("inventado" + i + "@mail.com", "ZZZZZZZZ", NUEVA);
        }

        assertThat(recuperar(ANA, suya.getCodigo(), NUEVA).estado()).isEqualTo(429);
    }

    @Test
    @DisplayName("R26: con la clave se da de baja, la sesión se cierra y sus reservas quedan")
    void seDaDeBaja() {
        Reserva suya = reservar(ANA, "A1");
        String cookie = entrar(ANA, CLAVE);

        Respuesta baja = conCookie(HttpMethod.DELETE, "/api/cuenta", cookie, "{\"actual\":\"" + CLAVE + "\"}");

        assertThat(baja.estado()).isEqualTo(204);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", cookie).estado()).isEqualTo(401);
        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE).estado()).isEqualTo(401);
        Respuesta reserva = get("/api/reservas/" + suya.getId());
        assertThat(reserva.estado()).isEqualTo(200);
        assertThat(reserva.json().get("estado").asText()).isEqualTo("RESERVADA");
    }

    @Test
    @DisplayName("R26 y R22: dada de baja, la cuenta se reabre con el código de una de sus entradas")
    void laBajaSeReabreConUnCodigo() {
        Reserva suya = reservar(ANA, "A1");
        conCookie(HttpMethod.DELETE, "/api/cuenta", entrar(ANA, CLAVE), "{\"actual\":\"" + CLAVE + "\"}");

        Respuesta alta = pedirComo(HttpMethod.POST, "/api/clientes", "{\"nombre\":\"Ana\",\"email\":\"" + ANA
                + "\",\"password\":\"" + NUEVA + "\",\"codigoReserva\":\"" + suya.getCodigo() + "\"}", null, null);

        assertThat(alta.estado()).isEqualTo(201);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", entrar(ANA, NUEVA)).json()).hasSize(1);
    }

    @Test
    @DisplayName("R26: con la clave equivocada no se da de baja")
    void laBajaExigeLaClave() {
        String cookie = entrar(ANA, CLAVE);

        Respuesta baja = conCookie(HttpMethod.DELETE, "/api/cuenta", cookie, "{\"actual\":\"no-es-esta\"}");

        assertThat(baja.estado()).isEqualTo(400);
        assertThat(baja.error()).isEqualTo("La contraseña actual no es correcta");
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", cookie).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("R27: el encargado asigna una clave temporal; el cliente entra con ella y se le pide cambiarla")
    void claveTemporal() {
        String sesionDeAntes = entrar(ANA, CLAVE);

        Respuesta asignada = post(claveTemporalDe(ANA), null);

        assertThat(asignada.estado()).isEqualTo(200);
        String temporal = asignada.json().get("clave").asText();
        assertThat(temporal).hasSize(10);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", sesionDeAntes).estado()).isEqualTo(401);
        Respuesta login = pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, temporal);
        assertThat(login.json().get("debeCambiarClave").asBoolean()).isTrue();

        conCookie(HttpMethod.POST, "/api/cuenta/clave", cookieDe(login),
                "{\"actual\":\"" + temporal + "\",\"nueva\":\"" + NUEVA + "\"}");

        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, NUEVA).json()
                .get("debeCambiarClave").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("R27: a quien compró sin cuenta la clave temporal se la abre")
    void claveTemporalAbreLaCuenta() {
        reservar("beto@mail.com", "A1");
        String temporal = post(claveTemporalDe("beto@mail.com"), null).json().get("clave").asText();

        assertThat(pedirComo(HttpMethod.POST, "/api/sesion", null, "beto@mail.com", temporal).estado())
                .isEqualTo(200);
    }

    @ParameterizedTest(name = "R27: {0}")
    @CsvSource(textBlock = """
            cliente inexistente, 99,   404
            el empleado,         9000, 404
            """)
    void claveTemporalDeQuienNoEsCliente(String caso, int id, int estado) {
        assertThat(post("/api/clientes/" + id + "/clave-temporal", null).estado()).isEqualTo(estado);
    }

    @Test
    @DisplayName("R27: la clave temporal es solo del encargado")
    void claveTemporalSoloDelEncargado() {
        Respuesta respuesta = conCookie(HttpMethod.POST, claveTemporalDe(ANA), entrar(ANA, CLAVE));

        assertThat(respuesta.estado()).isEqualTo(403);
    }

    private Reserva reservar(String email, String butaca) {
        return reservas.reservar(funcionId, "Cliente", email, Map.of(butaca, TipoTarifa.GENERAL), null);
    }

    private String claveTemporalDe(String email) {
        return "/api/clientes/" + get("/api/clientes?email=" + email).json().get(0).get("id").asInt() + "/clave-temporal";
    }

    private Respuesta recuperar(String email, String codigo, String nueva) {
        return pedirComo(HttpMethod.POST, "/api/cuenta/recuperacion",
                "{\"email\":\"" + email + "\",\"codigoReserva\":\"" + codigo + "\",\"nueva\":\"" + nueva + "\"}",
                null, null);
    }

    private String entrar(String email, String clave) {
        return cookieDe(pedirComo(HttpMethod.POST, "/api/sesion", null, email, clave));
    }

    private static String cookieDe(Respuesta respuesta) {
        String cookie = respuesta.cabeceras().getFirst(HttpHeaders.SET_COOKIE);
        return cookie.substring(0, cookie.indexOf(';'));
    }

    private Respuesta conCookie(HttpMethod metodo, String ruta, String cookie) {
        return conCookie(metodo, ruta, cookie, null);
    }

    private Respuesta conCookie(HttpMethod metodo, String ruta, String cookie, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.add(HttpHeaders.COOKIE, cookie);
        if (cuerpo != null) {
            cabeceras.setContentType(MediaType.APPLICATION_JSON);
        }
        ResponseEntity<String> respuesta = http.exchange(URI.create(ruta), metodo,
                new HttpEntity<>(cuerpo, cabeceras), String.class);
        return new Respuesta(respuesta.getStatusCode().value(), respuesta.getBody(), respuesta.getHeaders());
    }
}
