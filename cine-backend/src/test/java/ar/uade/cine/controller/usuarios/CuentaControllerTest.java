package ar.uade.cine.controller.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;

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

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

    private int funcionId;

    private MockHttpSession ana;

    @BeforeEach
    void unaFuncionYAnaConLaSesionDeGoogle() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        funcionId = funcion.getId();
        ana = entrarConGoogle(ANA);
    }

    @Test
    @DisplayName("R23: el cliente cambia su nombre con la sesión; el email queda")
    void cambiaSuNombre() {
        Respuesta respuesta = conSesion(HttpMethod.PUT, "/api/cuenta", ana,
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
        Respuesta respuesta = conSesion(HttpMethod.PUT, "/api/cuenta", ana, "{\"nombre\":" + nombre + "}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).isEqualTo(error);
    }

    @Test
    @DisplayName("R23: un nombre de más de 100 caracteres es 400, como en el alta")
    void nombreLargo() {
        Respuesta respuesta = conSesion(HttpMethod.PUT, "/api/cuenta", ana,
                "{\"nombre\":\"" + "a".repeat(101) + "\"}");

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).startsWith("El nombre");
    }

    @ParameterizedTest(name = "sin sesión: {0} /api/cuenta es 401")
    @CsvSource(textBlock = """
            PUT,    '{"nombre":"Ana"}'
            DELETE,
            """)
    void sinSesionEs401(HttpMethod metodo, String cuerpo) {
        assertThat(pedirComo(metodo, "/api/cuenta", cuerpo, null, null).estado()).isEqualTo(401);
    }

    // Swing manda Basic sin sesión: la cuenta es solo del cliente.
    @ParameterizedTest(name = "el empleado no usa /api/cuenta: {0} es 403")
    @CsvSource(textBlock = """
            PUT,    '{"nombre":"Otro"}'
            DELETE,
            """)
    void elEmpleadoNoTieneCuenta(HttpMethod metodo, String cuerpo) {
        assertThat(pedirComo(metodo, "/api/cuenta", cuerpo, EMAIL_ADMIN, CLAVE_ADMIN).estado()).isEqualTo(403);
    }

    // El código es lo único que hace falta para ver el ticket: con el email entero, quien lo vea sabría de quién es.
    @Test
    @DisplayName("el ticket público por código muestra el email enmascarado; el encargado lo ve entero")
    void elTicketPublicoEnmascaraElEmail() {
        Reserva suya = reservar(ANA, "A1");

        Respuesta publica = pedirComo(HttpMethod.GET, "/api/reservas/codigo/" + suya.getCodigo(), null, null, null);

        assertThat(publica.json().get("cliente").get("email").asText()).isEqualTo("a***@mail.com");
        assertThat(publica.cuerpo()).doesNotContain(ANA);
        assertThat(get("/api/reservas/" + suya.getId()).json().get("cliente").get("email").asText()).isEqualTo(ANA);
    }

    @Test
    @DisplayName("R26: sin pedir clave, la baja cierra la sesión y sus reservas quedan")
    void seDaDeBaja() {
        Reserva suya = reservar(ANA, "A1");

        Respuesta baja = conSesion(HttpMethod.DELETE, "/api/cuenta", ana, null);

        assertThat(baja.estado()).isEqualTo(204);
        assertThat(conSesion(HttpMethod.GET, "/api/reservas/mias", ana, null).estado()).isEqualTo(401);
        Respuesta reserva = get("/api/reservas/" + suya.getId());
        assertThat(reserva.estado()).isEqualTo(200);
        assertThat(reserva.json().get("estado").asText()).isEqualTo("RESERVADA");
    }

    @Test
    @DisplayName("R26 y R28: dada de baja, volver a entrar con Google la abre con sus reservas")
    void laBajaSeReabreConGoogle() {
        reservar(ANA, "A1");
        conSesion(HttpMethod.DELETE, "/api/cuenta", ana, null);

        assertThat(conSesion(HttpMethod.GET, "/api/reservas/mias", entrarConGoogle(ANA), null).json()).hasSize(1);
    }

    private Reserva reservar(String email, String butaca) {
        return reservas.reservar(funcionId, "Cliente", email, Map.of(butaca, TipoTarifa.GENERAL), null);
    }
}
