package ar.uade.cine.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.infrastructure.pasarelas.mercadopago.MercadoPagoHttp;
import ar.uade.cine.infrastructure.pasarelas.mercadopago.PropiedadesMercadoPago;

class AdaptadoresTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin token,        ,             false
            token en blanco,  '  ',         false
            con token,        TEST-123,     true
            """)
    void laPasarelaDeVerdadSoloConToken(String caso, String token, boolean deVerdad) {
        var pasarela = new Adaptadores().pasarelaPagos(
                new PropiedadesMercadoPago(token, "https://api.mercadopago.com", "http://localhost:8080"));

        assertEquals(deVerdad ? MercadoPagoHttp.class : MercadoPagoEmulado.class, pasarela.getClass(), caso);
    }
}
