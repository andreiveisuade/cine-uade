package ar.uade.cine.controller.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.GestorPagos;
import ar.uade.cine.service.ventas.GestorReservas;

class CuentaDelClienteTest extends PruebaDeApi {

    private static final String ANA = "ana@mail.com";
    private static final String QR = "{\"medio\":\"QR\"}";

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

    @Autowired
    private GestorPagos pagos;

    private int funcionId;

    @BeforeEach
    void unaFuncion() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
        funcionId = funcion.getId();
    }

    @Test
    @DisplayName("R28: quien reservó sin cuenta entra con Google con ese email y ve las reservas de antes")
    void elQueReservoSinCuentaEntraConGoogleYVeSusReservas() {
        Reserva deAntes = reservar("Beto", "beto@mail.com", "A1");

        Respuesta mias = conSesion(HttpMethod.GET, "/api/reservas/mias", entrarConGoogle("Beto@Mail.com"), null);

        assertThat(mias.json()).hasSize(1);
        assertThat(mias.json().get(0).get("id").asInt()).isEqualTo(deAntes.getId());
    }

    // Swing manda Basic en cada pedido: si cada uno abriera una sesión, el servidor juntaría miles.
    @Test
    @DisplayName("el login de un empleado sigue sin sesión: Swing manda Basic en cada pedido")
    void elLoginDelEmpleadoNoAbreSesion() {
        Respuesta login = pedirComo(HttpMethod.POST, "/api/sesion", null, EMAIL_ADMIN, CLAVE_ADMIN);

        assertThat(login.estado()).isEqualTo(200);
        assertThat(login.cabeceras().containsKey(HttpHeaders.SET_COOKIE)).isFalse();
    }

    @Test
    @DisplayName("mis reservas trae solo las del cliente de la sesión, con su código")
    void misReservasTraeSoloLasSuyas() {
        Reserva suya = reservar("Ana", ANA, "A1");
        reservar("Beto", "beto@mail.com", "A2");

        Respuesta mias = conSesion(HttpMethod.GET, "/api/reservas/mias", entrarConGoogle(ANA), null);

        assertThat(mias.estado()).isEqualTo(200);
        assertThat(mias.json()).hasSize(1);
        assertThat(mias.json().get(0).get("id").asInt()).isEqualTo(suya.getId());
        assertThat(mias.json().get(0).get("codigo").asText()).isEqualTo(suya.getCodigo());
    }

    @Test
    @DisplayName("sin sesión, mis reservas es 401")
    void misReservasSinSesionEs401() {
        assertThat(pedirComo(HttpMethod.GET, "/api/reservas/mias", null, null, null).estado()).isEqualTo(401);
    }

    @Test
    @DisplayName("R21: el cliente cancela su reserva con la sesión abierta")
    void cancelaLaSuya() {
        Reserva suya = reservar("Ana", ANA, "A1");

        Respuesta cancelada = conSesion(HttpMethod.POST, cancelacion(suya), entrarConGoogle(ANA), null);

        assertThat(cancelada.estado()).isEqualTo(200);
        assertThat(cancelada.json().get("estado").asText()).isEqualTo("CANCELADA");
    }

    // 404 y no 403: para el que no es el dueño, la reserva no existe.
    @Test
    @DisplayName("R21: la reserva de otro cliente es 404 y queda como estaba")
    void laDeOtroEs404() {
        Reserva ajena = reservar("Beto", "beto@mail.com", "A2");

        Respuesta respuesta = conSesion(HttpMethod.POST, cancelacion(ajena), entrarConGoogle(ANA), null);

        assertThat(respuesta.estado()).isEqualTo(404);
        assertThat(respuesta.error()).isEqualTo("No existe ninguna reserva con ese código");
        assertThat(get("/api/reservas/" + ajena.getId()).json().get("estado").asText()).isEqualTo("RESERVADA");
    }

    @Test
    @DisplayName("R21: sin sesión cancelar es 401, aunque se tenga el código")
    void sinSesionEs401() {
        Reserva suya = reservar("Ana", ANA, "A1");

        Respuesta respuesta = pedirComo(HttpMethod.POST, cancelacion(suya), null, null, null);

        assertThat(respuesta.estado()).isEqualTo(401);
        assertThat(respuesta.error()).isEqualTo("Hace falta iniciar sesión para esta operación");
    }

    // 400 y no 409: es el rechazo de R13 de siempre (EstadoReserva), el mismo que recibe el encargado.
    @Test
    @DisplayName("R13: el dueño tampoco cancela una reserva cobrada")
    void cobradaNoSeCancela() {
        Reserva suya = reservar("Ana", ANA, "A1");
        pagos.cobrar(suya.getId(), MedioPago.EFECTIVO, "");

        Respuesta respuesta = conSesion(HttpMethod.POST, cancelacion(suya), entrarConGoogle(ANA), null);

        assertThat(respuesta.estado()).isEqualTo(409);
        assertThat(respuesta.error()).isEqualTo("La reserva está pagada: solo se puede cancelar una reserva sin cobrar");
    }

    @Test
    @DisplayName("cerrar la sesión la deja sin efecto")
    void cerrarLaSesion() {
        MockHttpSession sesion = entrarConGoogle(ANA);

        assertThat(conSesion(HttpMethod.DELETE, "/api/sesion", sesion, null).estado()).isEqualTo(204);
        assertThat(conSesion(HttpMethod.GET, "/api/reservas/mias", sesion, null).estado()).isEqualTo(401);
    }

    @Test
    @DisplayName("con la sesión de cliente no se entra a lo del encargado")
    void elClienteNoEsEncargado() {
        Reserva suya = reservar("Ana", ANA, "A1");
        MockHttpSession sesion = entrarConGoogle(ANA);

        assertThat(conSesion(HttpMethod.GET, "/api/reservas", sesion, null).estado()).isEqualTo(403);
        assertThat(conSesion(HttpMethod.POST, "/api/reservas/" + suya.getId() + "/cancelacion", sesion, null).estado())
                .isEqualTo(403);
    }

    // El mismo camino que el checkout del encargado: mismo medio, mismo monto con descuento.
    @Test
    @DisplayName("el cliente abre el checkout de su reserva con la sesión, igual que el encargado por id")
    void abreElCheckoutDeLaSuya() {
        Reserva suya = reservar("Ana", ANA, "A1");

        Respuesta delCliente = conSesion(HttpMethod.POST, checkout(suya), entrarConGoogle(ANA), QR);
        Respuesta delEncargado = post("/api/reservas/" + suya.getId() + "/checkout", QR);

        assertThat(delCliente.estado()).isEqualTo(delEncargado.estado());
        assertThat(delCliente.json().get("reservaId").asInt()).isEqualTo(suya.getId());
        assertThat(delCliente.json().get("medio").asText()).isEqualTo("QR");
        assertThat(delCliente.json().get("monto").asDouble()).isEqualTo(delEncargado.json().get("monto").asDouble());
    }

    @Test
    @DisplayName("el checkout de la reserva de otro es 404, como cancelarla")
    void elCheckoutDeLaDeOtroEs404() {
        Reserva ajena = reservar("Beto", "beto@mail.com", "A2");

        Respuesta respuesta = conSesion(HttpMethod.POST, checkout(ajena), entrarConGoogle(ANA), QR);

        assertThat(respuesta.estado()).isEqualTo(404);
        assertThat(respuesta.error()).isEqualTo("No existe ninguna reserva con ese código");
    }

    @Test
    @DisplayName("sin sesión, el checkout por código es 401")
    void elCheckoutSinSesionEs401() {
        Reserva suya = reservar("Ana", ANA, "A1");

        assertThat(pedirComo(HttpMethod.POST, checkout(suya), QR, null, null).estado()).isEqualTo(401);
    }

    private static String checkout(Reserva reserva) {
        return "/api/reservas/codigo/" + reserva.getCodigo() + "/checkout";
    }

    private Reserva reservar(String nombre, String email, String butaca) {
        return reservas.reservar(funcionId, nombre, email, Map.of(butaca, TipoTarifa.GENERAL), null);
    }

    private static String cancelacion(Reserva reserva) {
        return "/api/reservas/codigo/" + reserva.getCodigo() + "/cancelacion";
    }
}
