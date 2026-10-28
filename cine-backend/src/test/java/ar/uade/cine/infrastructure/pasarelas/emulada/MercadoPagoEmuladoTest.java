package ar.uade.cine.infrastructure.pasarelas.emulada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ar.uade.cine.infrastructure.pasarelas.PasarelaPagos.Checkout;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;

class MercadoPagoEmuladoTest {

    // Relativo, para que el comprador no salga del host en el que está.
    @Test
    void elLinkLlevaALaPantallaDePruebaDelSitioConElMontoYElMedio() {
        Checkout checkout = new MercadoPagoEmulado()
                .crear(7, MedioPago.DEBITO, Dinero.de(4500.5), LocalDateTime.of(2026, 8, 14, 10, 30));

        assertEquals("/#/pasarela-emulada/" + checkout.id() + "?monto=4500.5&medio=DEBITO", checkout.urlPago());
    }
}
