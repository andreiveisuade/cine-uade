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
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;

class FuncionTest {

    private static final Pelicula MATRIX = new Pelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
    private static final LocalDateTime LAS_20 = LocalDateTime.of(2026, 8, 20, 20, 0);

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin idioma,                   ,            DOS_D,  DOS_D, 5000, Falta el idioma
            sin proyección,               SUBTITULADA, ,       DOS_D, 5000, Falta la proyección
            R8: 3D en una sala 2D,        SUBTITULADA, TRES_D, DOS_D, 5000, La sala Sala 1 no puede proyectar en 3D
            precio cero,                  SUBTITULADA, DOS_D,  DOS_D, 0,    El precio tiene que ser mayor a cero
            precio de cien millones,      SUBTITULADA, DOS_D,  DOS_D, 100000000, El precio no puede superar $ 1000000.00
            sin idioma y sin precio,      ,            DOS_D,  DOS_D, 0,    Falta el idioma
            """)
    void unaFuncionSinFormatoSinPrecioOEn3DSinSoporteNoSeConstruye(String caso, Version version,
            Proyeccion proyeccion, TipoSala tipo, double precio, String mensaje) {
        Sala sala = new Sala("Sala 1", tipo, 15);

        rechaza(mensaje, () -> new Funcion(MATRIX, sala, LAS_20, version, proyeccion, Dinero.de(precio)));
    }

    @Test
    void sinFechaYHoraNoSeConstruye() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("Falta la fecha y hora de la función",
                () -> new Funcion(MATRIX, sala, null, Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    // R20 es del alta, no de la función: la que quedó en el pasado es historial (R12).
    @Test
    void unaFuncionEnElPasadoEsValidaY3DEnUnaSalaQueLoSoportaTambien() {
        Sala imax = new Sala("Sala IMAX", TipoSala.IMAX, 15);

        assertDoesNotThrow(() -> new Funcion(MATRIX, imax, LocalDateTime.of(2020, 1, 1, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(5000)));
    }
}
