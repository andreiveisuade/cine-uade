package ar.uade.cine.model.tiempo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class FranjaHorariaTest {

    private static final LocalTime DOS = LocalTime.of(14, 0);
    private static final LocalTime SEIS = LocalTime.of(18, 0);

    // 22 a 2 cruza la medianoche: ninguna hora cumpliría desde ≤ hora ≤ hasta y la franja no correría nunca.
    @ParameterizedTest
    @CsvSource({"18:00, 14:00", "22:00, 02:00", "14:00, 14:00"})
    void sinEmpezarAntesDeTerminarSeRechaza(LocalTime desde, LocalTime hasta) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new FranjaHoraria(desde, hasta));

        assertEquals("La franja horaria tiene que empezar antes de terminar", error.getMessage());
    }

    @Test
    void incluyeLasDosPuntasYNadaAfuera() {
        FranjaHoraria tarde = new FranjaHoraria(DOS, SEIS);

        assertTrue(tarde.incluye(DOS));
        assertTrue(tarde.incluye(LocalTime.of(16, 30)));
        assertTrue(tarde.incluye(SEIS));
        assertFalse(tarde.incluye(DOS.minusMinutes(1)));
        assertFalse(tarde.incluye(SEIS.plusMinutes(1)));
    }

    // Como en las promociones: con una sola punta, la otra queda abierta; sin ninguna, es todo el día.
    @Test
    void lasPuntasSonOpcionales() {
        FranjaHoraria desdeLasDos = new FranjaHoraria(DOS, null);
        FranjaHoraria hastaLasSeis = new FranjaHoraria(null, SEIS);
        FranjaHoraria todoElDia = new FranjaHoraria(null, null);

        assertTrue(desdeLasDos.incluye(LocalTime.of(23, 59)));
        assertFalse(desdeLasDos.incluye(LocalTime.of(13, 59)));
        assertTrue(hastaLasSeis.incluye(LocalTime.MIDNIGHT));
        assertFalse(hastaLasSeis.incluye(LocalTime.of(18, 1)));
        assertTrue(todoElDia.incluye(LocalTime.MIDNIGHT));
        assertTrue(todoElDia.incluye(LocalTime.of(23, 59)));
    }
}
