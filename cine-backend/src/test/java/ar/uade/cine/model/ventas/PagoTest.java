package ar.uade.cine.model.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;

class PagoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 14, 10, 0);

    // Los importes salen de la reserva y de la promoción: si no cierran es un error del cálculo.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin medio de pago,              ,         10000,  0,     Falta el medio de pago
            subtotal negativo,              EFECTIVO, -1,     0,     El subtotal del pago tiene que ser mayor o igual a cero
            descuento negativo,             EFECTIVO, 10000,  -1,    El descuento tiene que ser mayor o igual a cero
            descuento mayor que el subtotal, EFECTIVO, 10000, 10001, El descuento tiene que ser menor o igual al subtotal
            """)
    void unPagoQueNoCierraSeRechaza(String caso, MedioPago medio, double subtotal, double descuento,
                                    String mensaje) {
        DatoInvalido error = assertThrows(DatoInvalido.class,
                () -> new Pago(1, Dinero.de(subtotal), null, Dinero.de(descuento), medio, AHORA, ""));

        assertEquals(mensaje, error.getMessage());
    }

    @Test
    void elDescuentoPuedeLlevarseTodoElSubtotal() {
        Pago pago = new Pago(1, Dinero.de(5000), 7, Dinero.de(5000), MedioPago.EFECTIVO, AHORA, "");

        assertEquals(Dinero.CERO, pago.getMonto());
    }

    // La autorización se arma al nacer el pago, con las reglas de su medio (R11).
    @Test
    void laAutorizacionLaArmaElPagoConLasReglasDeSuMedio() {
        Pago conTarjeta = new Pago(1, Dinero.de(5000), null, Dinero.CERO, MedioPago.CREDITO, AHORA, "  AUT-1  ");

        assertEquals("AUT-1", conTarjeta.getCodigoAutorizacion());
        assertEquals("Falta el código de autorización del pago con débito",
                assertThrows(DatoInvalido.class, () -> new Pago(1, Dinero.de(5000), null, Dinero.CERO,
                        MedioPago.DEBITO, AHORA, null)).getMessage());
    }
}
