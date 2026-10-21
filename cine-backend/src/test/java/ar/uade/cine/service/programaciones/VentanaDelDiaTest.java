package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;

class VentanaDelDiaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 9, 1);

    private static CriteriosGrilla deCatorceAVeintitres(int dias) {
        return new CriteriosGrilla(DIA, dias, LocalTime.of(14, 0), LocalTime.of(23, 0), 1,
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    // R20: el primer intento es la apertura, o el primer turno de media hora que todavía no pasó.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            un día que todavía no empezó arranca en la apertura,  2026-08-31T10:00, 2026-09-01T14:00
            hoy a media tarde arranca en el próximo turno,        2026-09-01T15:10, 2026-09-01T15:30
            hoy pasado el cierre no deja nada antes del cierre,   2026-09-01T23:40, 2026-09-01T23:00
            """)
    void elPrimerIntentoNoCaeEnElPasado(String caso, LocalDateTime ahora, LocalDateTime esperado) {
        VentanaDelDia ventana = new VentanaDelDia(deCatorceAVeintitres(1), ahora);

        assertEquals(esperado, ventana.primerIntento(DIA), caso);
    }

    @Test
    void lasFechasSonLosDiasQueCubreLaGrilla() {
        VentanaDelDia ventana = new VentanaDelDia(deCatorceAVeintitres(3), DIA.atStartOfDay());

        assertEquals(List.of(DIA, DIA.plusDays(1), DIA.plusDays(2)), ventana.fechas());
    }

    @Test
    void elCierreEsElDeLosCriterios() {
        VentanaDelDia ventana = new VentanaDelDia(deCatorceAVeintitres(1), DIA.atStartOfDay());

        assertEquals(DIA.atTime(23, 0), ventana.cierre(DIA));
    }
}
