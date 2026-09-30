package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatoTest {

    @Test
    void laHoraDelDiaSeVeSinSegundosVengaComoVenga() {
        assertEquals("20:30", Formato.horaDelDia("20:30:00"));
        assertEquals("09:05", Formato.horaDelDia("09:05"));
        assertEquals("—", Formato.horaDelDia(null));
    }

    @Test
    void laHoraDeUnMomentoIgnoraLaFecha() {
        assertEquals("20:30", Formato.hora("2026-08-13T20:30:00"));
    }

    @Test
    void elDiaSeNombraIgualDesdeLaFechaQueDesdeElMomento() {
        LocalDate hoy = LocalDate.now();
        assertEquals("Hoy", Formato.dia(hoy));
        assertEquals("Mañana", Formato.dia(hoy.plusDays(1)));
        assertEquals("jueves 13 ago", Formato.dia(LocalDate.of(2020, 8, 13)));
        assertEquals(Formato.dia(LocalDate.of(2020, 8, 13)), Formato.dia("2020-08-13T20:30:00"));
    }
}
