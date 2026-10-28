package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Las ventas de candy de un día: arranca por hoy y cambiar la fecha pide la de ese día.
class VentasDelDiaTest {

    private static final String VENTA = "{\"id\":11,\"reservaId\":5,\"fecha\":\"2026-11-06T20:00:00\","
            + "\"medio\":\"EFECTIVO\",\"items\":[{\"nombre\":\"Pochoclos\",\"cantidad\":2,\"subtotal\":6000.0}],"
            + "\"total\":6000.0,\"ahorro\":0.0}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/candy/compras?fecha=" + LocalDate.now(), 200, "[]");
        encargado = Operador.en(() -> new VentasDelDia(new ApiCandy(new ClienteHttp(servidor.url()))));
        encargado.esperarTexto("0 ventas");
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void otroDiaPideLasVentasDeEseDia() {
        servidor.responder("GET /api/candy/compras?fecha=2026-11-06", 200, "[" + VENTA + "]");

        encargado.fecha("Fecha", LocalDate.of(2026, 11, 6));

        encargado.esperarTexto("1 venta");
        assertEquals("#5", encargado.celda(0, "Reserva"));
        assertEquals("$ 6.000", encargado.celda(0, "Total"));
    }
}
