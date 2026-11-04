package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.List;

import static ar.uade.cine.swing.pantallas.ventas.Ventas.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// El listado de reservas: el resumen de lo pendiente, los filtros y cobrar solo lo que el backend dice cobrable.
class PantallaReservasTest {

    private static final String PAGO = "{\"reservaId\":6,\"subtotal\":7500.0,\"descuento\":0.0,\"monto\":7500.0,"
            + "\"medio\":\"EFECTIVO\",\"fecha\":\"2026-11-05T10:00:00\"}";

    private ServidorFalso servidor;
    private Operador encargado;
    private final Ventas.Anotadora navegacion = new Ventas.Anotadora();

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/reservas", 200, "[" + reserva(5, "RESERVADA", true, "null") + ","
                + reserva(6, "PAGADA", false, PAGO) + "," + reserva(7, "CANCELADA", false, "null") + "]");
        encargado = Operador.en(() -> new PantallaReservas(new ApiVentas(new ClienteHttp(servidor.url())),
                navegacion));
        encargado.esperarFilas(3);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void resumeLoQueFaltaCobrar() {
        encargado.esperarTexto("3 reservas · 2 activas · 1 pendiente de cobro");

        assertEquals("Matrix · viernes 6 nov 20:30 · Sala 1", encargado.celda(0, "Función"));
        assertEquals("A1, A2", encargado.celda(0, "Butacas"));
        assertEquals("Vence 10:30", encargado.celda(0, "Pago"));
        assertEquals("Pagada", encargado.celda(1, "Estado"));
        assertEquals("Efectivo 10:00 · entró 20:10", encargado.celda(1, "Pago"));
    }

    // R32: NO_USADA es la paga que nadie usó con la función terminada; llega así del backend y se muestra tal cual.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            vencida sin pagar, EXPIRADA, Vencida
            paga sin usar,     NO_USADA, Vencida sin usar
            """)
    void unaVencidaNoCuentaComoActiva(String caso, String estado, String etiqueta) {
        servidor.responder("GET /api/reservas", 200, "[" + reserva(5, "RESERVADA", true, "null") + ","
                + reserva(8, estado, false, "null") + "]");
        encargado.cerrar();

        encargado = Operador.en(() -> new PantallaReservas(new ApiVentas(new ClienteHttp(servidor.url())),
                navegacion));

        encargado.esperarTexto("2 reservas · 1 activa · 1 pendiente de cobro");
        assertEquals(etiqueta, encargado.celda(1, "Estado"));
    }

    @Test
    void loPendienteSumaElCandyPedidoEnLaWebQueSeCobraConLaReserva() {
        String candy = "[{\"id\":12,\"reservaId\":5,\"estado\":\"PENDIENTE\",\"fecha\":\"2026-11-05T09:50:00\","
                + "\"items\":[{\"nombre\":\"Pochoclos\",\"cantidad\":1,\"subtotal\":3000.0}],\"total\":3000.0,\"ahorro\":0.0}]";
        servidor.responder("GET /api/reservas", 200, "[" + reserva(5, "RESERVADA", true, "null")
                .replace("\"cancelable\":true}", "\"cancelable\":true,\"candy\":" + candy + "}") + "]");
        encargado.cerrar();

        encargado = Operador.en(() -> new PantallaReservas(new ApiVentas(new ClienteHttp(servidor.url())),
                navegacion));

        encargado.esperarTexto("1 pendiente de cobro ($ 10.500)");
        assertEquals("$ 10.500", encargado.celda(0, "Total"));
        assertEquals("1× Pochoclos", encargado.celda(0, "Candy"));
    }

    // Ya paga, la columna dice lo cobrado: la promo descuenta y el candy pendiente se paga aparte.
    @Test
    void elTotalDeUnaPagaEsLoCobradoConPromoYSinElCandyPendiente() {
        String pagoConPromo = PAGO.replace("\"descuento\":0.0,\"monto\":7500.0", "\"descuento\":3750.0,\"monto\":3750.0");
        String candy = "[{\"id\":1,\"estado\":\"COBRADA\",\"fecha\":\"2026-11-05T09:50:00\",\"items\":[],\"total\":5500.0},"
                + "{\"id\":2,\"estado\":\"PENDIENTE\",\"fecha\":\"2026-11-05T11:00:00\",\"items\":[],\"total\":2000.0}]";
        servidor.responder("GET /api/reservas", 200, "[" + reserva(6, "PAGADA", false, pagoConPromo)
                .replace("\"cancelable\":false}", "\"cancelable\":false,\"candy\":" + candy + "}") + "]");
        encargado.cerrar();

        encargado = Operador.en(() -> new PantallaReservas(new ApiVentas(new ClienteHttp(servidor.url())),
                navegacion));
        encargado.esperarFilas(1);

        assertEquals("$ 9.250", encargado.celda(0, "Total"));
    }

    @Test
    void soloSeCobraLaQueElBackendDiceCobrable() {
        assertFalse(encargado.habilitado("Cobrar"));

        encargado.elegirFila(1);
        assertFalse(encargado.habilitado("Cobrar"));

        encargado.elegirFila(0).clic("Cobrar");

        assertEquals(List.of("cobro 5"), navegacion.visitas);
    }

    @Test
    void filtrarPorEstadoPideEseEstado() {
        servidor.responder("GET /api/reservas?estado=PAGADA", 200, "[" + reserva(6, "PAGADA", false, PAGO) + "]");

        encargado.elegir("Estado", "Pagada");

        encargado.esperarFilas(1);
        encargado.esperarTexto("mostrando 1 de 3");
        assertEquals("6", encargado.celda(0, "#"));
    }
}
