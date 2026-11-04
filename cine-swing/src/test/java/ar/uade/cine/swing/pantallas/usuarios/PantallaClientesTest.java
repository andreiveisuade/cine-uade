package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El listado de clientes y, al elegir uno, sus transacciones: qué pide, cómo las ordena y qué total muestra.
class PantallaClientesTest {

    private static final String ANA = "{\"id\":5,\"nombre\":\"Ana Prueba\",\"email\":\"ana@mail.com\"}";
    private static final String ZOE = "{\"id\":6,\"nombre\":\"Zoe\",\"email\":\"zoe@mail.com\"}";
    private static final String RESERVA_PAGA = """
            {"id":9,"estado":"PAGADA","creadaEn":"2026-10-02T10:00:00","codigo":"ABC123","total":5000,
             "entradas":[{"codigo":"A1"},{"codigo":"A2"}],
             "pelicula":{"titulo":"Matrix"},"funcion":{"inicio":"2026-10-03T20:00:00"},"sala":{"nombre":"Sala 1"},
             "pago":{"monto":4500,"medio":"EFECTIVO","fecha":"2026-10-02T10:05:00"},
             "candy":[{"id":1,"estado":"COBRADA","total":1000,"items":[{"nombre":"Pochoclos","cantidad":1}]}]}""";
    private static final String RESERVA_CANCELADA = """
            {"id":8,"estado":"CANCELADA","creadaEn":"2026-09-01T10:00:00","codigo":"DEF456","total":3000,
             "entradas":[{"codigo":"B1"}],"pelicula":{"titulo":"Alien"}}""";
    private static final String CANDY_SUELTO = """
            {"id":2,"estado":"COBRADA","fecha":"2026-09-15T18:00:00","cobradaEn":"2026-09-15T18:00:00","total":7000,
             "items":[{"nombre":"Gaseosa","cantidad":2}]}""";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/clientes", 200, "[" + ANA + "," + ZOE + "]");
        encargado = Operador.en(() -> new PantallaClientes(new ApiClientes(new ClienteHttp(servidor.url()))));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void alEntrarListaATodosConSuEmail() {
        assertEquals("Ana Prueba", encargado.celda(0, "Nombre"));
        assertEquals("zoe@mail.com", encargado.celda(1, "Email"));
        encargado.esperarTexto("2 clientes");
    }

    @Test
    void elFiltroLePideAlBackendPorNombreOEmail() {
        servidor.responder("GET /api/clientes?q=ana", 200, "[" + ANA + "]");

        encargado.tipear("Buscar", "ana");

        encargado.esperarFilas(1);
        encargado.esperarTexto("mostrando 1 de 2");
    }

    @Test
    void alElegirUnoMuestraSusTransaccionesLaMasNuevaPrimeroConElTotalDelBackend() {
        servidor.responder("GET /api/clientes/5/transacciones", 200,
                "{\"reservas\":[" + RESERVA_PAGA + "," + RESERVA_CANCELADA + "],\"comprasCandy\":[" + CANDY_SUELTO + "]}");

        encargado.elegirFila(0);
        Operador transacciones = encargado.seccion("Transacciones");
        transacciones.esperarFilas(3);

        assertTrue(transacciones.celda(0, "Película o función").startsWith("Matrix"));
        assertEquals("A1, A2", transacciones.celda(0, "Butacas"));
        assertEquals("1× Pochoclos", transacciones.celda(0, "Candy"));
        assertEquals("Pagada", transacciones.celda(0, "Estado"));
        assertEquals("$ 5.500", transacciones.celda(0, "Total").replace('\u00a0', ' '));
        assertEquals("Candy", transacciones.celda(1, "Tipo"));
        assertEquals("2× Gaseosa", transacciones.celda(1, "Candy"));
        assertEquals("—", transacciones.celda(1, "Butacas"));
        assertEquals("Cancelada", transacciones.celda(2, "Estado"));
        encargado.esperarTexto("Ana Prueba · ana@mail.com · 3 transacciones");
    }

    @Test
    void elErrorDelBackendSeMuestraYNoQuedanLasDelClienteAnterior() {
        servidor.responder("GET /api/clientes/5/transacciones", 200,
                "{\"reservas\":[" + RESERVA_CANCELADA + "],\"comprasCandy\":[]}");
        servidor.responder("GET /api/clientes/6/transacciones", 404, "{\"error\":\"No existe el cliente 6\"}");
        encargado.elegirFila(0);
        Operador transacciones = encargado.seccion("Transacciones");
        transacciones.esperarFilas(1);

        encargado.elegirFila(1);

        transacciones.esperarFilas(0);
        encargado.esperarTexto("No existe el cliente 6");
    }
}
