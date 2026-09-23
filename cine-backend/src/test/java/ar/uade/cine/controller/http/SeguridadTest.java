package ar.uade.cine.controller.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

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

    private Reserva unaReserva() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        return reservas.reservar(funcion.getId(), "Andrei", "andrei@uade.edu.ar",
                Map.of("A1", TipoTarifa.GENERAL), null);
    }
}
