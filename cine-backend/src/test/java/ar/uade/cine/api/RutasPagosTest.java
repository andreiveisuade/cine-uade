package ar.uade.cine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.dominio.ventas.TipoTarifa;

/**
 * El cobro electrónico por HTTP: abrir el checkout y confirmarlo.
 *
 * <p>Es el único circuito del sistema que son <strong>dos</strong> pedidos, y por eso hace
 * falta probarlo así: lo que se prueba no es una llamada sino que el id que devuelve el
 * primero sirva para el segundo. Los mensajes de error se comparan enteros porque el
 * contrato dice que salen intactos desde el gestor y se le muestran al usuario tal cual.
 */
class RutasPagosTest {

    @TempDir
    Path tempDir;

    private ApiEnMemoria api;
    private Reserva reserva;

    /** Una reserva de dos butacas generales sin cobrar, a $5000 cada una. */
    @BeforeEach
    void levantarLaApiConUnaReservaSinCobrar() {
        api = new ApiEnMemoria(tempDir);
        Aplicacion app = api.aplicacion();

        app.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        app.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion funcion = app.getFunciones().programar(1, 1,
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        Cliente cliente = app.getClientes().identificar("Andrei", "andrei@uade.edu.ar");
        reserva = app.getReservas().reservar(funcion.getId(), cliente.getId(),
                Map.of("A1", TipoTarifa.GENERAL, "A2", TipoTarifa.GENERAL));
    }

    @AfterEach
    void bajarLaApi() {
        api.close();
    }

    @Test
    void abrirElCheckoutDevuelveElLinkYElQrConElMontoAPagar() {
        ApiEnMemoria.Respuesta respuesta = checkout("QR");

        assertEquals(201, respuesta.estado());
        var checkout = respuesta.json();
        assertEquals(reserva.getId(), checkout.get("reservaId").asInt());
        assertEquals("QR", checkout.get("medio").asText());
        assertEquals(10000.0, checkout.get("monto").asDouble(), 0.001);
        assertFalse(checkout.get("id").asText().isBlank());
        assertTrue(checkout.get("urlPago").asText().contains(checkout.get("id").asText()));
        assertFalse(checkout.get("codigoQr").asText().isBlank());
    }

    /** R11 al revés: el efectivo no tiene a quién pedirle una autorización. */
    @Test
    void elEfectivoNoAbreCheckout() {
        ApiEnMemoria.Respuesta respuesta = checkout("EFECTIVO");

        assertEquals(400, respuesta.estado());
        assertEquals("El pago con EFECTIVO se cobra en la caja del cine, no por checkout",
                respuesta.error());
    }

    @Test
    void sinMedioElErrorLoDaElGestorYNoLaCapaHttp() {
        ApiEnMemoria.Respuesta respuesta =
                api.post("/api/reservas/" + reserva.getId() + "/checkout", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el medio de pago", respuesta.error());
    }

    @Test
    void unMedioInventadoNoLlegaAlGestor() {
        ApiEnMemoria.Respuesta respuesta = checkout("CRIPTO");

        assertEquals(400, respuesta.estado());
        assertEquals("Valor inválido para el medio de pago: CRIPTO", respuesta.error());
    }

    @Test
    void elCheckoutDeUnaReservaQueNoExisteEs404() {
        ApiEnMemoria.Respuesta respuesta = api.post("/api/reservas/99/checkout", "{\"medio\":\"QR\"}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la reserva 99", respuesta.error());
    }

    /** El código no lo tipea nadie: sale de la pasarela y llega en el pago. */
    @Test
    void confirmarElCheckoutCobraYDevuelveElPagoAutorizado() {
        String id = checkout("QR").json().get("id").asText();

        ApiEnMemoria.Respuesta respuesta = api.post("/api/checkouts/" + id + "/confirmacion", "");

        assertEquals(201, respuesta.estado());
        var pago = respuesta.json();
        assertEquals(reserva.getId(), pago.get("reservaId").asInt());
        assertEquals("QR", pago.get("medio").asText());
        assertEquals(10000.0, pago.get("monto").asDouble(), 0.001);
        assertFalse(pago.get("codigoAutorizacion").asText().isBlank());
    }

    /** Un doble click no cobra dos veces: la segunda choca contra R5. */
    @Test
    void confirmarDosVecesElMismoCheckoutNoCobraDeNuevo() {
        String id = checkout("QR").json().get("id").asText();
        api.post("/api/checkouts/" + id + "/confirmacion", "");

        ApiEnMemoria.Respuesta segunda = api.post("/api/checkouts/" + id + "/confirmacion", "");

        assertEquals(400, segunda.estado());
        assertEquals("La reserva está PAGADA, no se puede cobrar", segunda.error());
        assertEquals(1, api.aplicacion().getPagos().listar().size());
    }

    @Test
    void confirmarUnCheckoutQueNoExisteEs400() {
        ApiEnMemoria.Respuesta respuesta =
                api.post("/api/checkouts/MP-0000000000/confirmacion", "");

        assertEquals(400, respuesta.estado());
        assertEquals("No existe el checkout MP-0000000000", respuesta.error());
    }

    private ApiEnMemoria.Respuesta checkout(String medio) {
        return api.post("/api/reservas/" + reserva.getId() + "/checkout",
                "{\"medio\":\"" + medio + "\"}");
    }
}
