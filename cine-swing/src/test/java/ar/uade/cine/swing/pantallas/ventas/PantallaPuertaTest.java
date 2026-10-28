package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static ar.uade.cine.swing.pantallas.ventas.Ventas.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;

// La puerta (CU-18): manda el código como se escaneó, dice si pasa y qué entradas piden carnet.
class PantallaPuertaTest {

    private ServidorFalso servidor;
    private Operador acomodador;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        ClienteHttp http = new ClienteHttp(servidor.url());
        acomodador = Operador.en(() -> new PantallaPuerta(new ApiVentas(http), new ApiCandy(http)));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        acomodador.cerrar();
    }

    @Test
    void unaEntradaValidaPasaYAvisaQueTarifaPideCarnet() {
        servidor.responder("POST /api/acceso", 200, reserva(5, "PAGADA", false, "null"));

        acomodador.tipear(" abc5 ").clic("Validar");

        acomodador.esperarTexto("ADELANTE");
        acomodador.esperarTexto("Jubilado · pedir carnet");
        acomodador.esperarTexto("2 personas");
        assertEquals(0, servidor.veces("GET /api/tarifas"), "pedir carnet lo dice la entrada");
        assertEquals("{\"codigo\":\"abc5\"}", servidor.ultimo("POST /api/acceso").cuerpo());
        assertEquals("", acomodador.valor("Validar entrada"), "el campo queda vacío para el siguiente");
    }

    @Test
    void unaYaUsadaNoPasaYDiceElMotivoDelBackend() {
        servidor.responder("POST /api/acceso", 409, "{\"error\":\"La entrada ya se usó a las 20:10\"}");

        acomodador.tipear("ABC5").clic("Validar");

        acomodador.esperarTexto("NO PASA");
        acomodador.esperarTexto("La entrada ya se usó a las 20:10");
    }

    @Test
    void sinCodigoNoSeManda() {
        acomodador.clic("Validar");

        acomodador.esperarTexto("Falta completar «Código»");
        assertEquals(0, servidor.veces("POST /api/acceso"));
    }

    // D7: el mismo código entrega el candy cobrado una sola vez; el segundo intento dice cuándo se retiró.
    @Test
    void conElMismoCodigoSeEntregaElCandy() {
        servidor.responder("POST /api/candy/retiros", 200, "[{\"id\":12,\"reservaId\":5,\"items\":"
                + "[{\"nombre\":\"Combo clásico\",\"cantidad\":2,\"subtotal\":11000.0}],\"total\":11000.0}]");

        acomodador.tipear("abc5").clic("Entregar candy");

        acomodador.esperarTexto("ENTREGAR");
        acomodador.esperarTexto("Combo clásico");
        assertEquals("{\"codigo\":\"abc5\"}", servidor.ultimo("POST /api/candy/retiros").cuerpo());
        assertEquals(0, servidor.veces("POST /api/acceso"), "entregar el candy no registra el ingreso");
    }

    @Test
    void elCandyYaRetiradoNoSeEntregaDeNuevo() {
        servidor.responder("POST /api/candy/retiros", 409,
                "{\"error\":\"El candy de esa entrada ya se retiró el 21/10 19:50\"}");

        acomodador.tipear("ABC5").clic("Entregar candy");

        acomodador.esperarTexto("NO SE ENTREGA");
        acomodador.esperarTexto("El candy de esa entrada ya se retiró el 21/10 19:50");
    }
}
