package ar.uade.cine.model.programaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
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
import ar.uade.cine.model.rechazos.Rechazo;
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
            # Una cerrada genera todo su rango de una vez: de 2026 a 9999 eran 2,9 millones de funciones.
            un año y un día,                   2026-01-01, 2027-01-02, 20:30, ,       El rango no puede cubrir más de 366 días
            hasta el año 9999,                 2026-09-07, 9999-12-31, 20:30, ,       El rango no puede cubrir más de 366 días
            hora con segundos,                 2026-09-07, 2026-09-13, 20:30:15, ,    La hora de la función tiene que ir sin segundos
            """)
    void unaGrillaSinFechaSinHoraOSinDiasEnSuRangoNoSeConstruye(String caso, LocalDate desde, LocalDate hasta,
            LocalTime hora, DayOfWeek dia, String mensaje) {
        Set<DayOfWeek> dias = dia == null ? Set.of() : Set.of(dia);

        assertEquals(mensaje, assertThrows(Rechazo.class,
                () -> grilla(desde, hasta, hora, dias)).getMessage());
    }

    // 366 y no 365: un año bisiesto entero también es un año.
    @Test
    void unRangoDeUnAnioBisiestoEnteroEsValido() {
        assertEquals(366, grilla(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31), LocalTime.of(20, 30), Set.of())
                .horarios(LocalDate.of(2028, 12, 31)).size());
    }

    // `[null]` llega del JSON como un día que no es ninguno.
    @Test
    void unDiaNuloNoSeConstruye() {
        Set<DayOfWeek> conNulo = new HashSet<>(Arrays.asList(DayOfWeek.MONDAY, null));

        assertEquals("Falta el día de la semana", assertThrows(Rechazo.class,
                () -> grilla(LocalDate.of(2026, 9, 7), null, LocalTime.of(20, 30), conNulo)).getMessage());
    }

    // El formato y el precio son los de las funciones que va a generar: se rechazan antes de la primera.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin idioma,            ,            DOS_D,  5000, Falta el idioma
            R8: 3D en una sala 2D, SUBTITULADA, TRES_D, 5000, La sala Sala 1 no puede proyectar en 3D
            precio cero,           SUBTITULADA, DOS_D,  0,    El precio tiene que ser mayor a cero
            """)
    void unaGrillaConUnFormatoOUnPrecioQueNoSirvenParaUnaFuncionNoSeConstruye(String caso, Version version,
            Proyeccion proyeccion, double precio, String mensaje) {
        assertEquals(mensaje, assertThrows(Rechazo.class,
                () -> new Programacion(MATRIX, SALA, LocalDate.of(2026, 9, 7), null, LocalTime.of(20, 30), Set.of(),
                        version, proyeccion, Dinero.de(precio))).getMessage());
    }

    // R20 y el horizonte miran el reloj: los pide el alta. El 14/08/2026 a las 10:00, un rango cerrado
    // que ya pasó entero no generaría nada, y uno que termina a más de un año generaría funciones que el
    // alta de cada una rechaza.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            ya pasó entero,              2026-08-10, 2026-08-14, 09:00, Todos los horarios del rango ya pasaron: la grilla no generaría funciones
            termina a un año y un día,   2027-01-01, 2027-08-15, 20:30, El rango tiene que terminar dentro del próximo año
            """)
    void elAltaRechazaUnRangoQueYaPasoOQueTerminaAMasDeUnAnio(String caso, LocalDate desde, LocalDate hasta,
            LocalTime hora, String mensaje) {
        Programacion cerrada = grilla(desde, hasta, hora, Set.of());

        assertEquals(mensaje, assertThrows(Rechazo.class,
                () -> cerrada.exigirGenerableA(LocalDateTime.of(2026, 8, 14, 10, 0))).getMessage());
    }

    @Test
    void elAltaAceptaUnaAbiertaLejanaYUnaCerradaQueTerminaJustoAlAnio() {
        LocalDateTime ahora = LocalDateTime.of(2026, 8, 14, 10, 0);

        assertDoesNotThrow(() -> grilla(LocalDate.of(2030, 1, 1), null, LocalTime.of(20, 30), Set.of())
                .exigirGenerableA(ahora));
        assertDoesNotThrow(() -> grilla(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 8, 14), LocalTime.of(20, 30),
                Set.of()).exigirGenerableA(ahora));
    }

    // Abierta no tiene fin contra el cual medir los días: algún lunes va a llegar.
    @Test
    void unaGrillaAbiertaGeneraSoloLosDiasElegidos() {
        Programacion abierta = grilla(LocalDate.of(2026, 9, 8), null, LocalTime.of(20, 30), Set.of(DayOfWeek.MONDAY));

        assertEquals(List.of(LocalDate.of(2026, 9, 14).atTime(20, 30)), abierta.horarios(LocalDate.of(2026, 9, 20)));
    }

    @Test
    void unaGrillaAbiertaLlegaHastaElHorizonteYUnaCerradaHastaSuFin() {
        LocalDate hoy = LocalDate.of(2026, 9, 1);

        assertEquals(LocalDate.of(2026, 9, 15), grilla(hoy, null, LocalTime.of(20, 30), Set.of()).topePara(hoy));
        assertEquals(LocalDate.of(2027, 1, 1),
                grilla(hoy, LocalDate.of(2027, 1, 1), LocalTime.of(20, 30), Set.of()).topePara(hoy));
    }

    // Lo que ya se procesó no vuelve a salir aunque haya chocado: se reintentaría siempre.
    @Test
    void loYaGeneradoNoVuelveASalirYAlLlegarAlTopeQuedaAlDia() {
        LocalDate hoy = LocalDate.of(2026, 9, 1);
        Programacion abierta = grilla(hoy, null, LocalTime.of(20, 30), Set.of());
        assertFalse(abierta.estaAlDia(hoy), "nunca generó nada");

        abierta.marcarGeneradaHasta(LocalDate.of(2026, 9, 3));

        assertEquals(List.of(LocalDate.of(2026, 9, 4).atTime(20, 30)),
                abierta.horariosSinGenerar(LocalDate.of(2026, 9, 4)));
        assertFalse(abierta.estaAlDia(hoy));
        abierta.marcarGeneradaHasta(abierta.topePara(hoy));
        assertTrue(abierta.estaAlDia(hoy));
    }
}
