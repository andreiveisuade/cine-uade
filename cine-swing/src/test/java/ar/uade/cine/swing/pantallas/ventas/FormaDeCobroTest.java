package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada forma de cobro por separado: qué muestra, si pregunta antes y qué le pide al backend. Sin ventanas. */
class FormaDeCobroTest {

    private static final Reserva RESERVA = new Reserva(7, "PENDIENTE", "ABC123", null, null, List.of(), 5000, null, null,
            null, null, null, true, true, List.of());

    private ServidorFalso servidor;
    private ApiVentas api;
    private final CajaQuieta caja = new CajaQuieta();

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        api = new ApiVentas(new ClienteHttp(servidor.url()));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "efectivo en caja,   EFECTIVO, false, CobroEnCaja",
            "credito por checkout, CREDITO, true,  CobroPorCheckout",
    })
    void elCatalogoEligeLaForma(String caso, String medio, boolean requiereAutorizacion, String forma) {
        assertEquals(forma, FormaDeCobro.para(new MedioPago(medio, requiereAutorizacion), api)
                .getClass().getSimpleName());
    }

    @Test
    void sinMedioElegidoSeCobraEnCaja() {
        assertInstanceOf(CobroEnCaja.class, FormaDeCobro.para(null, api));
    }

    @Test
    void enCajaPreguntaYRegistraElPago() {
        servidor.responder("POST /api/reservas/7/pago", 200,
                "{\"reservaId\":7,\"subtotal\":5000,\"descuento\":0,\"monto\":5000,\"medio\":\"EFECTIVO\"}");
        FormaDeCobro forma = FormaDeCobro.para(new MedioPago("EFECTIVO", false), api);

        forma.enviar(RESERVA, new MedioPago("EFECTIVO", false), caja);

        assertEquals("Registrar cobro", forma.boton());
        assertEquals(List.of("¿Registrar el cobro de la reserva #7 en efectivo? No se puede deshacer."),
                caja.preguntas);
        assertEquals("POST /api/reservas/7/pago", servidor.ultimo().metodo() + " " + servidor.ultimo().ruta());
        // El monto no viaja: lo calcula el backend con las promociones del medio.
        assertEquals("{\"medio\":\"EFECTIVO\",\"codigoAutorizacion\":\"\"}", servidor.ultimo().cuerpo());
        assertInstanceOf(Pago.class, caja.resultado);
    }

    @Test
    void enCajaSinConfirmarNoCobra() {
        caja.confirma = false;

        FormaDeCobro.para(new MedioPago("EFECTIVO", false), api)
                .enviar(RESERVA, new MedioPago("EFECTIVO", false), caja);

        assertEquals(0, caja.pedidos);
    }

    @Test
    void porCheckoutAbreElCheckoutSinPreguntar() {
        servidor.responder("POST /api/reservas/7/checkout", 200,
                "{\"id\":\"chk-1\",\"reservaId\":7,\"medio\":\"CREDITO\",\"monto\":5000}");
        FormaDeCobro forma = FormaDeCobro.para(new MedioPago("CREDITO", true), api);

        forma.enviar(RESERVA, new MedioPago("CREDITO", true), caja);

        assertEquals("Abrir checkout", forma.boton());
        assertTrue(caja.preguntas.isEmpty());
        assertEquals("POST /api/reservas/7/checkout", servidor.ultimo().metodo() + " " + servidor.ultimo().ruta());
        assertEquals("{\"medio\":\"CREDITO\"}", servidor.ultimo().cuerpo());
        assertInstanceOf(Checkout.class, caja.resultado);
    }

    // Corre el pedido en el mismo hilo y anota lo que pasó, en lugar de abrir diálogos.
    private static final class CajaQuieta implements CajaDeCobro {

        private final List<String> preguntas = new ArrayList<>();
        private boolean confirma = true;
        private int pedidos;
        private Object resultado;

        @Override
        public boolean confirmar(String pregunta, String si) {
            preguntas.add(pregunta);
            return confirma;
        }

        @Override
        public <T> void pedir(Callable<T> trabajo, Consumer<T> alTerminar) {
            pedidos++;
            try {
                alTerminar.accept(trabajo.call());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void cobrado(Pago pago) {
            resultado = pago;
        }

        @Override
        public void mostrarCheckout(Checkout checkout) {
            resultado = checkout;
        }
    }
}
