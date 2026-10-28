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

import static ar.uade.cine.swing.pantallas.ventas.Ventas.TARIFAS;
import static ar.uade.cine.swing.pantallas.ventas.Ventas.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;

// La puerta (CU-18): manda el código como se escaneó, dice si pasa y qué entradas piden carnet.
class PantallaPuertaTest {

    private ServidorFalso servidor;
    private Operador acomodador;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/tarifas", 200, TARIFAS);
        ClienteHttp http = new ClienteHttp(servidor.url());
        acomodador = Operador.en(() -> new PantallaPuerta(new ApiCatalogos(http), new ApiVentas(http)));
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
        assertEquals("{\"codigo\":\"ABC5\"}", servidor.ultimo("POST /api/acceso").cuerpo());
        assertEquals("", acomodador.valor("Validar entrada"), "el campo queda vacío para el siguiente");
    }

    @Test
    void unaYaUsadaNoPasaYDiceElMotivoDelBackend() {
        servidor.responder("POST /api/acceso", 400, "{\"error\":\"La entrada ya se usó a las 20:10\"}");

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
}
