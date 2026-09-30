package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class FechasTest {

    @Test
    void elFormatoEsIsoLocalSinZona() {
        assertEquals("2026-08-20T20:30:00",
                Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30, 0)));
    }

    @Test
    void losSegundosViajanSiempreAunqueSeanCero() {
        assertEquals("2026-08-20T20:30:00", Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30)));
        assertEquals(19, Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30)).length());
    }

    @Test
    void loQueNoPasoTodaviaViajaComoNull() {
        assertNull(Fechas.texto(null));
    }

    @Test
    void elMesYElDiaVanConCeroAdelante() {
        assertEquals("2026-01-05T09:07:03",
                Fechas.texto(LocalDateTime.of(2026, 1, 5, 9, 7, 3)));
    }
}
