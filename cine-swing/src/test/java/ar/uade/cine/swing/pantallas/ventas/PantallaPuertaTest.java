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
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static ar.uade.cine.swing.pantallas.ventas.Ventas.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// La puerta (CU-18 y D7): un modo por canje, el mismo código; dice si pasa, qué pide carnet y qué candy se entrega.
class PantallaPuertaTest {

    private static final String CANDY_ENTREGADO = "[{\"id\":12,\"reservaId\":5,\"items\":"
            + "[{\"nombre\":\"Agua\",\"cantidad\":2,\"subtotal\":3000.0},"
            + "{\"nombre\":\"Combo clásico\",\"cantidad\":1,\"subtotal\":5500.0}],\"total\":8500.0}]";

    private ServidorFalso servidor;
    private ClienteHttp http;
    private Preferences preferencias;
    private Operador acomodador;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        http = new ClienteHttp(servidor.url());
        // Un nodo propio por test: no pisa el modo que eligió quien corre los tests en su compu.
        preferencias = Preferences.userRoot().node("/ar/uade/cine/swing/test-" + UUID.randomUUID());
        acomodador = abrir();
    }

    @AfterEach
    void bajar() throws BackingStoreException {
        servidor.bajar();
        acomodador.cerrar();
        preferencias.removeNode();
    }

    private Operador abrir() {
        return Operador.en(() -> new PantallaPuerta(new ApiVentas(http), new ApiCandy(http), preferencias));
    }

    @Test
    void unaEntradaValidaPasaYAvisaQueTarifaPideCarnet() {
        servidor.responder("POST /api/acceso", 200, reserva(5, "PAGADA", false, "null"));

        acomodador.tipear(" abc5 ").clic("Validar entrada");

        acomodador.esperarTexto("ENTRADA VÁLIDA");
        acomodador.esperarTexto("Jubilado · pedir carnet");
        acomodador.esperarTexto("2 personas");
        assertEquals(0, servidor.veces("GET /api/tarifas"), "pedir carnet lo dice la entrada");
        assertEquals("{\"codigo\":\"abc5\"}", servidor.ultimo("POST /api/acceso").cuerpo());
        assertEquals("abc5", acomodador.valor("Puerta").strip(),
                "el código queda, elegido: el próximo escaneo lo reemplaza y sirve para el otro modo");
    }

    @Test
    void siLaEntradaTraeCandyCobradoAvisaQueSeRetiraEnModoCandy() {
        String candy = "[{\"id\":12,\"reservaId\":5,\"estado\":\"COBRADA\",\"fecha\":\"2026-11-05T09:50:00\","
                + "\"items\":[{\"nombre\":\"Combo clásico\",\"cantidad\":1,\"subtotal\":5500.0}],"
                + "\"total\":5500.0,\"ahorro\":0.0}]";
        servidor.responder("POST /api/acceso", 200, reserva(5, "PAGADA", false, "null")
                .replace("\"cancelable\":false}", "\"cancelable\":false,\"candy\":" + candy + "}"));

        acomodador.tipear("abc5").clic("Validar entrada");

        acomodador.esperarTexto("Tiene candy para retirar:</b> pasá a modo Candy (1 × Combo clásico)");
        assertEquals(0, servidor.veces("POST /api/candy/retiros"), "avisa, pero no lo entrega");
    }

    @Test
    void unaYaUsadaNoPasaYDiceElMotivoDelBackend() {
        servidor.responder("POST /api/acceso", 409, "{\"error\":\"La entrada ya se usó a las 20:10\"}");

        acomodador.tipear("ABC5").clic("Validar entrada");

        acomodador.esperarTexto("ENTRADA NO VÁLIDA");
        acomodador.esperarTexto("La entrada ya se usó a las 20:10");
    }

    @Test
    void sinCodigoNoSeManda() {
        acomodador.clic("Validar entrada");

        acomodador.esperarTexto("Falta completar «Código»");
        assertEquals(0, servidor.veces("POST /api/acceso"));
    }

    @Test
    void elModoCambiaElBotonYElCanje() {
        servidor.responder("POST /api/candy/retiros", 200, CANDY_ENTREGADO);
        assertFalse(acomodador.hayBoton("Entregar candy"), "arranca en Entrada");

        acomodador.clic("Candy");
        assertFalse(acomodador.hayBoton("Validar entrada"));
        acomodador.tipear("abc5").clic("Entregar candy");

        acomodador.esperarTexto("CANDY ENTREGADO: 2 × Agua, 1 × Combo clásico");
        assertEquals("{\"codigo\":\"abc5\"}", servidor.ultimo("POST /api/candy/retiros").cuerpo());
        assertEquals(0, servidor.veces("POST /api/acceso"), "entregar el candy no registra el ingreso");
    }

    @Test
    void elEnterDelLectorCanjeaEnElModoElegido() {
        servidor.responder("POST /api/acceso", 200, reserva(5, "PAGADA", false, "null"));
        servidor.responder("POST /api/candy/retiros", 200, CANDY_ENTREGADO);

        acomodador.tipear("abc5").enter();
        acomodador.esperarTexto("ENTRADA VÁLIDA");
        acomodador.clic("Candy").enter();
        acomodador.esperarTexto("CANDY ENTREGADO");

        assertEquals(1, servidor.veces("POST /api/acceso"));
        assertEquals(1, servidor.veces("POST /api/candy/retiros"));
    }

    @Test
    void elCandyYaRetiradoNoSeEntregaDeNuevo() {
        servidor.responder("POST /api/candy/retiros", 409,
                "{\"error\":\"El candy de esa entrada ya se retiró el 21/10 19:50\"}");

        acomodador.clic("Candy").tipear("ABC5").clic("Entregar candy");

        acomodador.esperarTexto("CANDY NO ENTREGADO");
        acomodador.esperarTexto("El candy de esa entrada ya se retiró el 21/10 19:50");
    }

    @Test
    void elModoElegidoSeRecuerdaParaLaProximaVez() {
        acomodador.clic("Candy");
        acomodador.cerrar();

        acomodador = abrir();

        assertTrue(acomodador.hayBoton("Entregar candy"));
        assertFalse(acomodador.hayBoton("Validar entrada"));
    }
}
