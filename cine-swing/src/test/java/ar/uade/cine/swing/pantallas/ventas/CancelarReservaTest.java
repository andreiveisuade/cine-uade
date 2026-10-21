package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Json;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Cancelar pregunta con "Volver" como no, y se habilita solo si el backend dice que se puede (R13, R17, R19).
class CancelarReservaTest {

    private ServidorFalso servidor;
    private CancelarReserva accion;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        accion = new CancelarReserva(new ApiVentas(new ClienteHttp(servidor.url())));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @Test
    void preguntaYCancela() {
        servidor.responder("POST /api/reservas/3/cancelacion", 200, "{\"id\":3,\"estado\":\"CANCELADA\"}");
        Reserva r = Json.leer("{\"id\":3,\"cancelable\":true}", Reserva.class);

        var pregunta = accion.pregunta(r).orElseThrow();
        accion.ejecutar(r);

        assertEquals("¿Cancelar la reserva #3? Las butacas quedan libres.", pregunta.texto());
        assertEquals("Sí, cancelar la reserva", pregunta.si());
        assertEquals("Volver", pregunta.no());
        assertEquals("POST", servidor.ultimo().metodo());
        assertEquals("/api/reservas/3/cancelacion", servidor.ultimo().ruta());
        assertEquals("Reserva cancelada, las butacas quedaron libres", accion.hecho(r));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "reservada se puede cancelar,      true",
            "pagada o vencida no se cancela,   false"})
    void seHabilitaSegunElBackend(String caso, boolean cancelable) {
        Reserva r = Json.leer("{\"id\":3,\"cancelable\":" + cancelable + "}", Reserva.class);

        assertEquals(cancelable, accion.habilitada(r));
    }
}
