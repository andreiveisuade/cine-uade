package ar.uade.cine.model.funciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;

class FuncionTest {

    private static final Pelicula MATRIX = new Pelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
    private static final LocalDateTime LAS_20 = LocalDateTime.of(2026, 8, 20, 20, 0);

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(Rechazo.class, accion).getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin idioma,                   ,            DOS_D,  DOS_D, 5000, Falta el idioma
            sin proyección,               SUBTITULADA, ,       DOS_D, 5000, Falta la proyección
            R8: 3D en una sala 2D,        SUBTITULADA, TRES_D, DOS_D, 5000, La sala Sala 1 no puede proyectar en 3D
            precio cero,                  SUBTITULADA, DOS_D,  DOS_D, 0,    El precio tiene que ser mayor a cero
            precio de cien millones,      SUBTITULADA, DOS_D,  DOS_D, 100000000, El precio no puede superar $ 1000000.00
            sin idioma y sin precio,      ,            DOS_D,  DOS_D, 0,    Falta el idioma
            R8 y precio cero gana R8,     SUBTITULADA, TRES_D, DOS_D, 0,    La sala Sala 1 no puede proyectar en 3D
            """)
    void unaFuncionSinFormatoSinPrecioOEn3DSinSoporteNoSeConstruye(String caso, Version version,
            Proyeccion proyeccion, TipoSala tipo, double precio, String mensaje) {
        Sala sala = new Sala("Sala 1", tipo, 15);

        rechaza(mensaje, () -> new Funcion(MATRIX, sala, LAS_20, version, proyeccion, Dinero.de(precio)));
    }

    // Las 20:30:46 no se anuncian en ninguna cartelera.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin fecha y hora, ,                              Falta la fecha y hora de la función
            con segundos,     2026-08-20T20:30:46,           La hora de la función tiene que ir sin segundos
            con nanos,        2026-08-20T20:30:00.000000001, La hora de la función tiene que ir sin segundos
            """)
    void unaFuncionSinFechaYHoraOFueraDelMinutoExactoNoSeConstruye(String caso, LocalDateTime inicio,
            String mensaje) {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza(mensaje,
                () -> new Funcion(MATRIX, sala, inicio, Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    // R20 con el corte de R19: la que empieza en este instante ya empezó. El horizonte se mide en días:
    // una función del 20/08/2026 entra si hoy es el 20/08/2025, a cualquier hora, y no si es el 19.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            en el minuto en que empieza, 2026-08-20T20:00, La función no puede empezar en el pasado
            ya empezada,                 2026-08-20T20:01, La función no puede empezar en el pasado
            a un año y un día,           2025-08-19T23:59, La función tiene que empezar dentro del próximo año
            """)
    void elAltaRechazaLaQueYaEmpezoYLaQueEstaAMasDeUnAnio(String caso, LocalDateTime ahora, String mensaje) {
        rechaza(mensaje, () -> deLas20().exigirProgramableA(ahora));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            un minuto antes de empezar,     2026-08-20T19:59
            el mismo día del año anterior,  2025-08-20T00:00
            """)
    void elAltaAceptaLaQueNoEmpezoDentroDelAnio(String caso, LocalDateTime ahora) {
        assertDoesNotThrow(() -> deLas20().exigirProgramableA(ahora));
    }

    private static Funcion deLas20() {
        return new Funcion(MATRIX, new Sala("Sala 1", TipoSala.DOS_D, 15), LAS_20,
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    // R20 es del alta, no de la función: la que quedó en el pasado es historial (R12).
    @Test
    void unaFuncionEnElPasadoEsValidaY3DEnUnaSalaQueLoSoportaTambien() {
        Sala imax = new Sala("Sala IMAX", TipoSala.IMAX, 15);

        assertDoesNotThrow(() -> new Funcion(MATRIX, imax, LocalDateTime.of(2020, 1, 1, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000)));
    }
}
