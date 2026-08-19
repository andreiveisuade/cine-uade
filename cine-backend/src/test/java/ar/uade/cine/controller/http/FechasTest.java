package ar.uade.cine.controller.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * El formato en que viajan las fechas es un contrato con el front: si una respuesta lo
 * cambiara, el front lo parsearía mal en esa pantalla y en ninguna otra. Es la clase de
 * error que se descubre tarde y en producción, así que se fija acá.
 */
class FechasTest {

    @Test
    void elFormatoEsIsoLocalSinZona() {
        assertEquals("2026-08-20T20:30:00",
                Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30, 0)));
    }

    /**
     * ISO_LOCAL_DATE_TIME omite los segundos cuando son cero, y ahí el front recibe dos
     * largos distintos para el mismo campo. Por eso el formato es explícito y no el que
     * trae Java de fábrica.
     */
    @Test
    void losSegundosViajanSiempreAunqueSeanCero() {
        assertEquals("2026-08-20T20:30:00", Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30)));
        assertEquals(19, Fechas.texto(LocalDateTime.of(2026, 8, 20, 20, 30)).length());
    }

    /** null es "todavía no pasó" —una reserva sin ingresar—, no una fecha vacía. */
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
