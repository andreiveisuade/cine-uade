package ar.uade.cine.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
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

    // En Render el contenedor corre en UTC: la hora del cine no puede depender de eso.
    @Test
    void elRelojDaLaHoraDeBuenosAiresAunqueLaJvmEsteEnUtc() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalDateTime ahora = new Adaptadores().reloj().ahora();
            LocalDateTime enBuenosAires = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));

            assertTrue(Duration.between(ahora, enBuenosAires).abs().toSeconds() < 5, ahora + " vs " + enBuenosAires);
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
