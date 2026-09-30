package ar.uade.cine.service.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.tiempo.Periodo;

class CriteriosGrillaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 14);

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin fecha de inicio,               ,           7,  14:00, 23:00, 8, 5000, Falta la fecha de inicio de la grilla
            cero días,                         2026-09-01, 0,  14:00, 23:00, 8, 5000, La grilla tiene que cubrir al menos un día
            más de un mes,                     2026-09-01, 32, 14:00, 23:00, 8, 5000, La grilla no puede cubrir más de 31 días
            sin películas,                     2026-09-01, 7,  14:00, 23:00, 0, 5000, Hay que programar al menos una película
            precio cero,                       2026-09-01, 7,  14:00, 23:00, 8, 0,    El precio tiene que ser mayor a cero
            precio de cien millones,           2026-09-01, 7,  14:00, 23:00, 8, 100000000, El precio no puede superar $ 1000000.00
            cierra antes de abrir,             2026-09-01, 7,  23:00, 14:00, 8, 5000, El cine tiene que cerrar después de abrir
            sin días ni precio gana el primero, 2026-09-01, 0,  14:00, 23:00, 8, 0,    La grilla tiene que cubrir al menos un día
            # Pasadas las veinte, a alguna le toca menos de un pase por día aun con cuatro salas.
            veintiuna películas,               2026-09-01, 7,  14:00, 23:00, 21, 5000, La grilla no puede tener más de 20 películas
            # Los pases arrancan en la apertura: con segundos, ninguno sería una función válida.
            apertura con segundos,             2026-09-01, 7,  14:00:30, 23:00, 8, 5000, La hora de apertura tiene que ir sin segundos
            """)
    void unosCriteriosImposiblesNoSeConstruyen(String caso, LocalDate desde, int dias, LocalTime apertura,
            LocalTime cierre, int cuantas, double precio, String mensaje) {
        assertEquals(mensaje, assertThrows(Rechazo.class,
                () -> new CriteriosGrilla(desde, dias, apertura, cierre, cuantas, Dinero.de(precio),
                        Version.SUBTITULADA, Proyeccion.DOS_D)).getMessage());
    }

    // hoy es el 14/08/2026. Una grilla que empieza en el pasado daba 201 sin crear nada (R20), y una que
    // termina a más de un año se proponía entera y después no se podía aplicar.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            empieza ayer,                   2026-08-13, 7,  La grilla no puede empezar en el pasado
            termina a un año y un día,      2027-08-09, 7,  La grilla tiene que terminar dentro del próximo año
            """)
    void completarRechazaLaQueEmpiezaEnElPasadoOTerminaAMasDeUnAnio(String caso, LocalDate desde, int dias,
            String mensaje) {
        assertEquals(mensaje, assertThrows(Rechazo.class,
                () -> CriteriosGrilla.completando(HOY, desde, dias, null, null, null, Dinero.de(5000), null, null))
                .getMessage());
    }

    @Test
    void sinDatosCompletaUnaSemanaDesdeHoyDe14A0ConOchoSubtituladasEn2D() {
        CriteriosGrilla semana = CriteriosGrilla.completando(HOY, null, null, null, null, null, Dinero.de(5000),
                null, null);

        assertEquals(new CriteriosGrilla(HOY, 7, LocalTime.of(14, 0), LocalTime.MIDNIGHT, 8, Dinero.de(5000),
                Version.SUBTITULADA, Proyeccion.DOS_D), semana);
        assertEquals(new Periodo(HOY, LocalDate.of(2026, 8, 20)), semana.periodo());
    }

    @ParameterizedTest(name = "cerrar a las {0} el 03/09 es cerrar el {1}")
    @CsvSource(textBlock = """
            00:00, 2026-09-04T00:00
            23:00, 2026-09-03T23:00
            """)
    void cerrarALaMedianocheEsCerrarAlEmpezarElDiaSiguiente(LocalTime cierre, LocalDateTime esperado) {
        CriteriosGrilla semana = new CriteriosGrilla(LocalDate.of(2026, 9, 1), 7, LocalTime.of(14, 0), cierre,
                8, Dinero.de(5000), Version.SUBTITULADA, Proyeccion.DOS_D);

        assertEquals(esperado, semana.cierreDe(LocalDate.of(2026, 9, 3)));
    }
}
