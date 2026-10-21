package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** El adaptador ida y vuelta: lo que se pone como LocalDate se lee igual, y vacío es null. Corre headless. */
class CampoFechaTest {

    @Test
    void loQueSePoneSeLeeIgualYViajaEnIso() {
        CampoFecha campo = new CampoFecha(null);

        campo.poner(LocalDate.of(2026, 8, 13));

        assertEquals(LocalDate.of(2026, 8, 13), campo.valor());
        assertEquals("2026-08-13", campo.iso());
    }

    @Test
    void vacioEsNull() {
        CampoFecha campo = new CampoFecha(LocalDate.of(2026, 8, 13));

        campo.poner(null);

        assertNull(campo.valor());
        assertNull(campo.iso());
    }

    @Test
    void avisaCuandoCambiaLaFecha() {
        CampoFecha campo = new CampoFecha(LocalDate.of(2026, 8, 13));
        AtomicInteger cambios = new AtomicInteger();
        campo.alCambiar(cambios::incrementAndGet);

        campo.poner(LocalDate.of(2026, 8, 14));

        assertEquals(1, cambios.get());
    }

    @Test
    void elCalendarioTambienVaYVuelve() {
        Calendario calendario = new Calendario(LocalDate.of(2026, 8, 13));

        calendario.poner(LocalDate.of(2026, 9, 1));

        assertEquals(LocalDate.of(2026, 9, 1), calendario.valor());
    }
}
