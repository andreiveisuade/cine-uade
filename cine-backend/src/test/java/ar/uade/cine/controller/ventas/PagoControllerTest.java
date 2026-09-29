package ar.uade.cine.controller.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.model.dinero.Dinero;

class PagoControllerTest extends PruebaDeApi {

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorClientes clientes;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

    @Autowired
    private GestorSalas salas;


    private Reserva reserva;

    @BeforeEach
    void levantarLaApiConUnaReservaSinCobrar() {

        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = funciones.programar(1, 1,
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
        Cliente cliente = clientes.identificar("Andrei", "andrei@uade.edu.ar");
        reserva = reservas.reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL, "A2", TipoTarifa.GENERAL), null);
    }


    @Test
    void abrirElCheckoutDevuelveElLinkYElQrConElMontoAPagar() {
        Respuesta respuesta = checkout("QR");

        assertEquals(201, respuesta.estado());
        var checkout = respuesta.json();
        assertEquals(reserva.getId(), checkout.get("reservaId").asInt());
        assertEquals("QR", checkout.get("medio").asText());
        assertEquals(10000.0, checkout.get("monto").asDouble(), 0.001);
        assertFalse(checkout.get("id").asText().isBlank());
        assertTrue(checkout.get("urlPago").asText().contains(checkout.get("id").asText()));
        assertFalse(checkout.get("codigoQr").asText().isBlank());
    }

    @Test
    void elEfectivoNoAbreCheckout() {
        Respuesta respuesta = checkout("EFECTIVO");

        assertEquals(400, respuesta.estado());
        assertEquals("El pago con EFECTIVO se cobra en la caja del cine, no por checkout",
                respuesta.error());
    }

    @Test
    void sinMedioEs400ConElMismoMensajeQueElGestor() {
        Respuesta respuesta =
                post("/api/reservas/" + reserva.getId() + "/checkout", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el medio de pago", respuesta.error());
    }

    @Test
    void unMedioInventadoNoLlegaAlGestor() {
        Respuesta respuesta = checkout("CRIPTO");

        assertEquals(400, respuesta.estado());
        assertEquals("Valor inválido para el medio de pago: CRIPTO", respuesta.error());
    }

    @Test
    void elCheckoutDeUnaReservaQueNoExisteEs404() {
        Respuesta respuesta = post("/api/reservas/99/checkout", "{\"medio\":\"QR\"}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la reserva 99", respuesta.error());
    }

    @Test
    void elPagoDeUnaReservaSinCobrarEsNullYElDeUnaQueNoExisteEs404() {
        Respuesta sinCobrar = get("/api/reservas/" + reserva.getId() + "/pago");
        Respuesta inexistente = get("/api/reservas/99/pago");

        assertEquals(200, sinCobrar.estado());
        assertEquals("null", sinCobrar.cuerpo());
        assertEquals(404, inexistente.estado());
        assertEquals("No existe la reserva 99", inexistente.error());
    }

    @Test
    void confirmarElCheckoutCobraYDevuelveElPagoAutorizado() {
        String id = checkout("QR").json().get("id").asText();

        Respuesta respuesta = post("/api/checkouts/" + id + "/confirmacion", "");

        assertEquals(201, respuesta.estado());
        var pago = respuesta.json();
        assertEquals(reserva.getId(), pago.get("reservaId").asInt());
        assertEquals("QR", pago.get("medio").asText());
        assertEquals(10000.0, pago.get("monto").asDouble(), 0.001);
        assertFalse(pago.get("codigoAutorizacion").asText().isBlank());
    }

    @Test
    void confirmarDosVecesElMismoCheckoutNoCobraDeNuevo() {
        String id = checkout("QR").json().get("id").asText();
        post("/api/checkouts/" + id + "/confirmacion", "");

        Respuesta segunda = post("/api/checkouts/" + id + "/confirmacion", "");

        assertEquals(400, segunda.estado());
        assertEquals("La reserva está PAGADA, no se puede cobrar", segunda.error());
        assertEquals(1, get("/api/arqueo?fecha=" + reloj.hoy()).json().get("pagos").size());
    }

    @Test
    void confirmarUnCheckoutQueNoExisteEs404() {
        Respuesta respuesta =
                post("/api/checkouts/MP-0000000000/confirmacion", "");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe el checkout MP-0000000000", respuesta.error());
    }

    private Respuesta checkout(String medio) {
        return post("/api/reservas/" + reserva.getId() + "/checkout",
                "{\"medio\":\"" + medio + "\"}");
    }

    // Los flags salen del mismo método de Reserva que usa el gestor: si dicen que no, el cobro rechaza.
    @Test
    void laReservaDiceSiSePuedeCobrarYCancelarConLasMismasReglasQueElGestor() {
        String ruta = "/api/reservas/" + reserva.getId();
        assertTrue(get(ruta).json().get("cobrable").asBoolean());
        assertTrue(get(ruta).json().get("cancelable").asBoolean());

        reloj.mover(reserva.getCreadaEn().plusMinutes(Reserva.MINUTOS_PARA_PAGAR + 1));
        assertFalse(get(ruta).json().get("cobrable").asBoolean(), "R17: vencida");
        assertTrue(get(ruta).json().get("cancelable").asBoolean(), "sigue RESERVADA hasta que alguien la expire");
        assertEquals(400, post(ruta + "/pago", "{\"medio\":\"EFECTIVO\"}").estado());

        reloj.reiniciar();
        post(ruta + "/pago", "{\"medio\":\"EFECTIVO\"}");
        assertFalse(get(ruta).json().get("cobrable").asBoolean(), "R5: ya cobrada");
        assertFalse(get(ruta).json().get("cancelable").asBoolean(), "R13: cobrada no se cancela");
        assertEquals(400, post(ruta + "/cancelacion", "").estado());
    }

    // La web vuelve al mapa recargado solo ante un 409: con un 400 el cliente quedaba trabado.
    @Test
    void reservarUnaButacaYaVendidaEs409YUnaFueraDeServicio400() {
        salas.marcarFueraDeServicio(1, "B1");

        Respuesta vendida = post("/api/reservas", "{\"funcionId\":1,\"nombre\":\"Ana\",\"email\":\"ana@mail.com\","
                + "\"butacas\":{\"A1\":\"GENERAL\"}}");
        Respuesta fueraDeServicio = post("/api/reservas", "{\"funcionId\":1,\"nombre\":\"Ana\","
                + "\"email\":\"ana@mail.com\",\"butacas\":{\"B1\":\"GENERAL\"}}");

        assertEquals(409, vendida.estado());
        assertEquals("La butaca A1 ya está ocupada", vendida.error());
        assertEquals(400, fueraDeServicio.estado());
        assertEquals("La butaca B1 está fuera de servicio", fueraDeServicio.error());
    }

    // Regresión: salía 409 y la web vaciaba la selección como si otro hubiera ganado la butaca.
    @Test
    void reservarConElEmailDeUnEmpleadoEs400() {
        Respuesta respuesta = post("/api/reservas", "{\"funcionId\":1,\"nombre\":\"Ana\",\"email\":\""
                + EMAIL_ADMIN + "\",\"butacas\":{\"B1\":\"GENERAL\"}}");

        assertEquals(400, respuesta.estado());
        assertEquals("Ese email es de un empleado del cine: usá otro para comprar", respuesta.error());
    }

    @Test
    void conLaFuncionEmpezadaLaReservaNoEsCobrable() {
        // Reservada diez minutos antes: al empezar la función todavía no venció.
        reloj.mover(LocalDateTime.of(2026, 8, 20, 19, 50));
        int id = post("/api/reservas", "{\"funcionId\":1,\"nombre\":\"Ana\",\"email\":\"ana@mail.com\","
                + "\"butacas\":{\"B1\":\"GENERAL\"}}").json().get("id").asInt();
        reloj.mover(LocalDateTime.of(2026, 8, 20, 20, 0));
        String ruta = "/api/reservas/" + id;

        assertFalse(get(ruta).json().get("cobrable").asBoolean(), "R19");
        assertEquals("La función ya empezó: no se puede cobrar la reserva " + id,
                post(ruta + "/pago", "{\"medio\":\"EFECTIVO\"}").error());
    }
}
