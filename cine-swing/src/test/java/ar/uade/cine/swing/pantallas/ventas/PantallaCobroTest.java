package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static ar.uade.cine.swing.pantallas.ventas.Ventas.MEDIOS;
import static ar.uade.cine.swing.pantallas.ventas.Ventas.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// La pantalla de cobro de una reserva: qué muestra según su estado y cómo abre el checkout de un medio con pasarela.
class PantallaCobroTest {

    private static final String PAGO = "{\"reservaId\":5,\"subtotal\":7500.0,\"descuento\":750.0,\"monto\":6750.0,"
            + "\"medio\":\"CREDITO\",\"fecha\":\"2026-11-05T10:00:00\"}";

    private ServidorFalso servidor;
    private Operador encargado;
    private final Ventas.Anotadora navegacion = new Ventas.Anotadora();

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/medios-pago", 200, MEDIOS);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    private void abrir(int reservaId) {
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaCobro(new ApiCatalogos(http), new ApiVentas(http), navegacion,
                reservaId));
    }

    @Test
    void unaReservaCobrableMuestraLoQueSeCobraYLaTarifaQuePideCarnet() {
        servidor.responder("GET /api/reservas/5", 200, reserva(5, "RESERVADA", true, "null"));
        abrir(5);

        encargado.esperarFilas(2);
        encargado.esperarTexto("Ana Prueba  ·  ana@mail.com");
        assertEquals("Jubilado · se acredita en la puerta", encargado.celda(1, "Tarifa"));
        assertEquals(0, servidor.veces("GET /api/tarifas"), "si se acredita lo dice la entrada");
        assertTrue(encargado.textos().stream().anyMatch(t -> t.startsWith("Subtotal")));
        assertTrue(encargado.hayBoton("Registrar cobro"), "el primer medio, efectivo, se cobra en caja");
    }

    // R30: lo pedido en la web se ve en el detalle y se cobra con las entradas: el backend lo cobra en el mismo pago.
    @Test
    void elCandyPedidoEnLaWebSeMuestraYSeCobraJunto() {
        String candy = "[{\"id\":12,\"reservaId\":5,\"estado\":\"PENDIENTE\",\"fecha\":\"2026-11-05T09:50:00\","
                + "\"codigoAutorizacion\":\"\",\"items\":[{\"nombre\":\"Combo clásico\",\"cantidad\":2,"
                + "\"subtotal\":11000.0}],\"total\":11000.0,\"ahorro\":3000.0}]";
        servidor.responder("GET /api/reservas/5", 200,
                reserva(5, "RESERVADA", true, "null").replace("\"cancelable\":true}", "\"cancelable\":true,\"candy\":" + candy + "}"));
        abrir(5);

        encargado.esperarTexto("Candy pedido en la web");
        encargado.esperarTexto("Candy  $ 11.000");
        encargado.esperarTexto("$ 18.500");
        encargado.esperarTexto("Las butacas y el candy pedido en la web se cobran juntos");
    }

    @Test
    void unaYaPagadaDiceCuantoYConQueSeCobro() {
        servidor.responder("GET /api/reservas/5", 200, reserva(5, "PAGADA", false, PAGO));
        abrir(5);

        encargado.esperarTexto("La reserva 5 no se puede cobrar: está pagada. Se cobró");
        encargado.esperarTexto("con Crédito (subtotal");
    }

    @Test
    void unaVencidaDiceQuePasoElPlazo() {
        servidor.responder("GET /api/reservas/5", 200,
                reserva(5, "RESERVADA", false, "null").replace("\"cancelable\":false", "\"cancelable\":true"));
        abrir(5);

        encargado.esperarTexto("venció el plazo para pagarla o la función ya empezó.");
    }

    @Test
    void unaQueNoExisteLoDiceEnLaPantalla() {
        servidor.responder("GET /api/reservas/99", 404, "{\"error\":\"No existe la reserva 99\"}");
        abrir(99);

        encargado.esperarTexto("No existe la reserva 99");
    }

    @Test
    void conTarjetaAbreElCheckoutConElQrYElLink() {
        servidor.responder("GET /api/reservas/5", 200, reserva(5, "RESERVADA", true, "null"));
        servidor.responder("POST /api/reservas/5/checkout", 201, "{\"id\":\"chk-9\",\"reservaId\":5,"
                + "\"medio\":\"CREDITO\",\"monto\":6750.0,\"urlPago\":\"https://pasarela.test/chk-9\","
                + "\"codigoQr\":\"QR-chk-9\"}");
        abrir(5);
        encargado.esperarFilas(2);

        encargado.elegir("Medio de pago", "Crédito").clic("Abrir checkout");

        encargado.esperarTexto("Checkout abierto · Crédito");
        assertEquals("{\"medio\":\"CREDITO\"}", servidor.ultimo("POST /api/reservas/5/checkout").cuerpo());
        assertEquals(List.of("QR-chk-9", "https://pasarela.test/chk-9"), encargado.areas());
        assertTrue(encargado.hayBoton("El cliente pagó · confirmar"));

        encargado.elegir("Medio de pago", "Efectivo");

        assertFalse(encargado.hayBoton("El cliente pagó · confirmar"), "cambiar el medio invalida el checkout");
    }

    @Test
    void elRechazoDelCheckoutSeMuestraJuntoAlBoton() {
        servidor.responder("GET /api/reservas/5", 200, reserva(5, "RESERVADA", true, "null"));
        servidor.responder("POST /api/reservas/5/checkout", 400, "{\"error\":\"La reserva ya tiene un pago\"}");
        abrir(5);
        encargado.esperarFilas(2);

        encargado.elegir("Medio de pago", "Crédito").clic("Abrir checkout");

        encargado.esperarTexto("La reserva ya tiene un pago");
        encargado.esperar(() -> encargado.habilitado("Abrir checkout"));
    }

    @Test
    void volverLlevaAlListadoDeReservas() {
        servidor.responder("GET /api/reservas/5", 200, reserva(5, "RESERVADA", true, "null"));
        abrir(5);

        encargado.clic("← Reservas");

        assertEquals(List.of("RESERVAS"), navegacion.visitas);
    }
}
