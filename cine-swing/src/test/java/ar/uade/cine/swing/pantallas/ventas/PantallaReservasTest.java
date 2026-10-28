package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        encargado.esperarTexto("3 reservas · 2 activas · 1 pendientes de cobro");

        assertEquals("Matrix · viernes 6 nov 20:30 · Sala 1", encargado.celda(0, "Función"));
        assertEquals("A1, A2", encargado.celda(0, "Butacas"));
        assertEquals("Pagada · Efectivo 10:00", encargado.celda(1, "Estado"));
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
