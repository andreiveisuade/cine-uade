package ar.uade.cine.model.tiempo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.DatoInvalido;

class PeriodoTest {

    private static final LocalDate PRIMERO = LocalDate.of(2026, 9, 1);
    private static final LocalDate TREINTA = LocalDate.of(2026, 9, 30);

    // Cada entidad nombra el suyo: la promoción tiene vigencia, la grilla un rango, la declaración un período.
    @ParameterizedTest
    @ValueSource(strings = {"La vigencia", "El rango", "El período"})
    void alRevesSeRechazaNombrandoQueEs(String sujeto) {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> Periodo.de(TREINTA, PRIMERO, sujeto));

        assertEquals(sujeto + " tiene que empezar antes de terminar", error.getMessage());
    }

    @Test
    void sinNombreEsElPeriodo() {
        DatoInvalido error = assertThrows(DatoInvalido.class, () -> new Periodo(TREINTA, PRIMERO));

        assertEquals("El período tiene que empezar antes de terminar", error.getMessage());
    }

    @Test
    void incluyeLasDosPuntasYNadaAfuera() {
        Periodo septiembre = Periodo.de(PRIMERO, TREINTA, "La vigencia");

        assertTrue(septiembre.incluye(PRIMERO));
        assertTrue(septiembre.incluye(LocalDate.of(2026, 9, 15)));
        assertTrue(septiembre.incluye(TREINTA));
        assertFalse(septiembre.incluye(PRIMERO.minusDays(1)));
        assertFalse(septiembre.incluye(TREINTA.plusDays(1)));
    }

    @Test
    void unSoloDiaEsUnPeriodoValido() {
        assertTrue(new Periodo(PRIMERO, PRIMERO).incluye(PRIMERO));
    }

    // La grilla abierta no tiene hasta: sigue generando funciones sin fin.
    @Test
    void unaPuntaEnNullQuedaAbierta() {
        Periodo sinFin = new Periodo(PRIMERO, null);
        Periodo sinInicio = new Periodo(null, TREINTA);

        assertTrue(sinFin.incluye(LocalDate.of(2099, 1, 1)));
        assertFalse(sinFin.incluye(PRIMERO.minusDays(1)));
        assertTrue(sinInicio.incluye(LocalDate.of(2000, 1, 1)));
        assertFalse(sinInicio.incluye(TREINTA.plusDays(1)));
    }

    @Test
    void esUnValor() {
        assertEquals(new Periodo(PRIMERO, TREINTA), Periodo.de(PRIMERO, TREINTA, "La vigencia"));
    }
}
