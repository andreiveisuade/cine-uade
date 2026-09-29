package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;

class CriteriosGrillaTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin fecha de inicio,               ,           7,  14:00, 23:00, 8, 5000, Falta la fecha de inicio de la grilla
            cero días,                         2026-09-01, 0,  14:00, 23:00, 8, 5000, La grilla tiene que cubrir al menos un día
            más de un mes,                     2026-09-01, 32, 14:00, 23:00, 8, 5000, La grilla no puede cubrir más de 31 días
            sin películas,                     2026-09-01, 7,  14:00, 23:00, 0, 5000, Hay que programar al menos una película
            precio cero,                       2026-09-01, 7,  14:00, 23:00, 8, 0,    El precio debe ser mayor a cero
            cierra antes de abrir,             2026-09-01, 7,  23:00, 14:00, 8, 5000, El cine tiene que cerrar después de abrir
            sin días ni precio gana el primero, 2026-09-01, 0,  14:00, 23:00, 8, 0,    La grilla tiene que cubrir al menos un día
            """)
    void unosCriteriosImposiblesNoSeConstruyen(String caso, LocalDate desde, int dias, LocalTime apertura,
            LocalTime cierre, int cuantas, double precio, String mensaje) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> new CriteriosGrilla(desde, dias, apertura, cierre, cuantas, Dinero.de(precio),
                        Version.SUBTITULADA, Proyeccion.DOS_D)).getMessage());
    }

    @Test
    void cerrarALaMedianocheEsCerrarAlFinalDelDia() {
        CriteriosGrilla semana = CriteriosGrilla.deUnaSemana(LocalDate.of(2026, 9, 1), 8, Dinero.de(5000));

        assertEquals(LocalTime.of(23, 59), semana.cierreEfectivo());
    }
}
