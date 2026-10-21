package ar.uade.cine.controller.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
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
    private static final String CLAVE = "clave-de-ana";
    private static final String QR = "{\"medio\":\"QR\"}";

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

    @Autowired
    private GestorPagos pagos;

    private int funcionId;

    @BeforeEach
    void unaFuncionYAnaConCuenta() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        funcionId = funcion.getId();
        pedirComo(HttpMethod.POST, "/api/clientes",
                "{\"nombre\":\"Ana\",\"email\":\"" + ANA + "\",\"password\":\"" + CLAVE + "\"}", null, null);
    }

    @Test
    @DisplayName("registrarse con un email que ya tiene cuenta es 409")
    void registrarseDosVecesEs409() {
        Respuesta respuesta = pedirComo(HttpMethod.POST, "/api/clientes",
                "{\"nombre\":\"Otra\",\"email\":\"" + ANA + "\",\"password\":\"otra-clave\"}", null, null);

        assertThat(respuesta.estado()).isEqualTo(409);
        assertThat(respuesta.error()).isEqualTo("Ya existe una cuenta con ese email");
    }

    @Test
    @DisplayName("quien reservó sin cuenta se registra con ese email y ve las reservas de antes")
    void elQueReservoSinCuentaReclamaSuEmail() {
        Reserva deAntes = reservar("Beto", "beto@mail.com", "A1");

        Respuesta alta = pedirComo(HttpMethod.POST, "/api/clientes",
                "{\"nombre\":\"Beto\",\"email\":\"beto@mail.com\",\"password\":\"clave-de-beto\"}", null, null);
        assertThat(alta.estado()).isEqualTo(201);

        Respuesta mias = conCookie(HttpMethod.GET, "/api/reservas/mias", entrar("beto@mail.com", "clave-de-beto"));
        assertThat(mias.json()).hasSize(1);
        assertThat(mias.json().get(0).get("id").asInt()).isEqualTo(deAntes.getId());
    }

    @Test
    @DisplayName("el login del cliente abre una sesión con cookie HttpOnly y sin nada sensible en el cuerpo")
    void elLoginDelClienteAbreUnaSesion() {
        Respuesta login = pedirComo(HttpMethod.POST, "/api/sesion", null, ANA, CLAVE);

        assertThat(login.estado()).isEqualTo(200);
        assertThat(login.json().get("rol").asText()).isEqualTo("CLIENTE");
        assertThat(login.cuerpo()).doesNotContain("password");
        String cookie = login.cabeceras().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("JSESSIONID=").contains("HttpOnly").contains("SameSite=Lax");
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

        Respuesta mias = conCookie(HttpMethod.GET, "/api/reservas/mias", entrar(ANA, CLAVE));

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

        Respuesta cancelada = conCookie(HttpMethod.POST, cancelacion(suya), entrar(ANA, CLAVE));

        assertThat(cancelada.estado()).isEqualTo(200);
        assertThat(cancelada.json().get("estado").asText()).isEqualTo("CANCELADA");
    }

    // 404 y no 403: para el que no es el dueño, la reserva no existe.
    @Test
    @DisplayName("R21: la reserva de otro cliente es 404 y queda como estaba")
    void laDeOtroEs404() {
        Reserva ajena = reservar("Beto", "beto@mail.com", "A2");

        Respuesta respuesta = conCookie(HttpMethod.POST, cancelacion(ajena), entrar(ANA, CLAVE));

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

        Respuesta respuesta = conCookie(HttpMethod.POST, cancelacion(suya), entrar(ANA, CLAVE));

        assertThat(respuesta.estado()).isEqualTo(400);
        assertThat(respuesta.error()).isEqualTo("La reserva está pagada: solo se puede cancelar una reserva sin cobrar");
    }

    @Test
    @DisplayName("cerrar la sesión deja la cookie sin efecto")
    void cerrarLaSesion() {
        String cookie = entrar(ANA, CLAVE);

        assertThat(conCookie(HttpMethod.DELETE, "/api/sesion", cookie).estado()).isEqualTo(204);
        assertThat(conCookie(HttpMethod.GET, "/api/reservas/mias", cookie).estado()).isEqualTo(401);
    }

    @Test
    @DisplayName("con la sesión de cliente no se entra a lo del encargado")
    void elClienteNoEsEncargado() {
        Reserva suya = reservar("Ana", ANA, "A1");
        String cookie = entrar(ANA, CLAVE);

        assertThat(conCookie(HttpMethod.GET, "/api/reservas", cookie).estado()).isEqualTo(403);
        assertThat(conCookie(HttpMethod.POST, "/api/reservas/" + suya.getId() + "/cancelacion", cookie).estado())
                .isEqualTo(403);
    }

    // El mismo camino que el checkout del encargado: mismo medio, mismo monto con descuento.
    @Test
    @DisplayName("el cliente abre el checkout de su reserva con la sesión, igual que el encargado por id")
    void abreElCheckoutDeLaSuya() {
        Reserva suya = reservar("Ana", ANA, "A1");

        Respuesta delCliente = conCookie(HttpMethod.POST, checkout(suya), entrar(ANA, CLAVE), QR);
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

        Respuesta respuesta = conCookie(HttpMethod.POST, checkout(ajena), entrar(ANA, CLAVE), QR);

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

    private String entrar(String email, String clave) {
        String cookie = pedirComo(HttpMethod.POST, "/api/sesion", null, email, clave)
                .cabeceras().getFirst(HttpHeaders.SET_COOKIE);
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
