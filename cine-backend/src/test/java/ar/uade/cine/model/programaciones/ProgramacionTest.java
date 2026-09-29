package ar.uade.cine.model.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;

class ProgramacionTest {

    private static final Pelicula MATRIX = new Pelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
    private static final Sala SALA = new Sala("Sala 1", TipoSala.DOS_D, 15);

    private static Programacion grilla(LocalDate desde, LocalDate hasta, LocalTime hora, Set<DayOfWeek> dias) {
        return new Programacion(MATRIX, SALA, desde, hasta, hora, dias, Version.SUBTITULADA, Proyeccion.DOS_D,
                Dinero.de(5000));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin fecha de inicio,               ,           2026-09-13, 20:30, ,       Falta la fecha de inicio
            rango al revés,                    2026-09-13, 2026-09-07, 20:30, ,       El rango tiene que empezar antes de terminar
            sin hora,                          2026-09-07, 2026-09-13, ,      ,       Falta la hora de la función
            del martes al jueves solo lunes,   2026-09-08, 2026-09-10, 20:30, MONDAY, Ningún día del rango cae en los días elegidos: la grilla no generaría funciones
            rango al revés y sin hora,         2026-09-13, 2026-09-07, ,      ,       El rango tiene que empezar antes de terminar
            """)
    void unaGrillaSinFechaSinHoraOSinDiasEnSuRangoNoSeConstruye(String caso, LocalDate desde, LocalDate hasta,
            LocalTime hora, DayOfWeek dia, String mensaje) {
        Set<DayOfWeek> dias = dia == null ? Set.of() : Set.of(dia);

        assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                () -> grilla(desde, hasta, hora, dias)).getMessage());
    }

    // Abierta no tiene fin contra el cual medir los días: algún lunes va a llegar.
    @Test
    void unaGrillaAbiertaGeneraSoloLosDiasElegidos() {
        Programacion abierta = grilla(LocalDate.of(2026, 9, 8), null, LocalTime.of(20, 30), Set.of(DayOfWeek.MONDAY));

        assertEquals(List.of(LocalDate.of(2026, 9, 14).atTime(20, 30)), abierta.horarios(LocalDate.of(2026, 9, 20)));
    }
}
