package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static ar.uade.cine.swing.pantallas.candy.CartaTest.COMBO;
import static ar.uade.cine.swing.pantallas.candy.CartaTest.POCHOCLOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// La venta de mostrador sin productos elegidos (con productos pregunta antes, y eso es un diálogo): qué manda y qué
// muestra, y el código de autorización según el medio.
class VentaMostradorTest {

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/candy/productos", 200, "[" + POCHOCLOS + "," + COMBO + "]");
        servidor.responder("GET /api/medios-pago", 200, "[{\"nombre\":\"EFECTIVO\",\"requiereAutorizacion\":false},"
                + "{\"nombre\":\"DEBITO\",\"requiereAutorizacion\":true}]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new VentaMostrador(new ApiCandy(http), new ApiCatalogos(http),
                new ApiClientes(http)));
        encargado.esperarTexto("2× Pochoclos + 1× Gaseosa");
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"Efectivo, false", "Débito, true"})
    void elCodigoDeAutorizacionSePideSoloSiElMedioLoRequiere(String medio, boolean pide) {
        encargado.elegir("Medio de pago", medio);

        assertEquals(pide, encargado.habilitado("Código de autorización"));
    }

    @Test
    void sinProductosElegidosElRechazoDelBackendQuedaJuntoAlFormulario() {
        servidor.responder("POST /api/candy/compras", 400, "{\"error\":\"La venta no tiene productos\"}");

        encargado.clic("Cobrar");

        encargado.esperarTexto("La venta no tiene productos");
        assertEquals("{\"cantidades\":{},\"medio\":\"EFECTIVO\",\"codigoAutorizacion\":\"\"}",
                servidor.ultimo("POST /api/candy/compras").cuerpo());
    }

    @Test
    void elClienteSeBuscaPorEmailYViajaSuId() {
        servidor.responder("GET /api/clientes?email=ana%40mail.com", 200,
                "[{\"id\":5,\"nombre\":\"Ana Prueba\",\"email\":\"ana@mail.com\"}]");
        servidor.responder("POST /api/candy/compras", 201, "{\"id\":11,\"fecha\":\"2026-11-06T20:00:00\","
                + "\"medio\":\"DEBITO\",\"codigoAutorizacion\":\"A1B2\",\"items\":[],\"total\":0.0,\"ahorro\":0.0}");
        encargado.elegir("Medio de pago", "Débito")
                .tipear("Código de autorización", " A1B2 ")
                .tipear("Cliente (opcional)", "ana@mail.com")
                .clic("Cobrar");

        encargado.esperarTexto("Venta #11");
        assertEquals("{\"clienteId\":5,\"cantidades\":{},\"medio\":\"DEBITO\",\"codigoAutorizacion\":\"A1B2\"}",
                servidor.ultimo("POST /api/candy/compras").cuerpo());
        encargado.esperarTexto("Cobrado $ 0");
        assertTrue(encargado.areas().get(0).contains("A1B2"), "el ticket muestra la autorización");
    }

    @Test
    void unEmailQueNoEsDeNingunClienteNoLlegaACobrar() {
        servidor.responder("GET /api/clientes?email=nadie%40mail.com", 200, "[]");

        encargado.tipear("Cliente (opcional)", "nadie@mail.com").clic("Cobrar");

        encargado.esperarTexto("No hay ningún cliente con el email nadie@mail.com");
        assertEquals(0, servidor.veces("POST /api/candy/compras"));
    }

    @Test
    void unEmailMalEscritoNoSeManda() {
        encargado.tipear("Cliente (opcional)", "ana@").clic("Cobrar");

        encargado.esperarTexto("«Cliente»: «ana@» no es un email");
        assertEquals(0, servidor.veces("POST /api/candy/compras"));
    }
}
