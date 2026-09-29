package ar.uade.cine.service.funciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;

class GestorFuncionesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorClientes clientes;

    @BeforeEach
    void prepararCartelera() {
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
    }

    private void cargarMasFunciones() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 22, 18, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    // R20
    @Test
    void noSeProgramaUnaFuncionEnElPasado() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, reloj.ahora().minusMinutes(1),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertEquals("La función no puede empezar en el pasado", error.getMessage());
        assertEquals(1, funciones.listar().size(), "solo la del arranque");
    }

    // R20 con el corte de R19: la que empieza en este instante ya empezó.
    @Test
    void ahoraMismoYaCuentaComoPasadoYUnMinutoDespuesNo() {
        assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, reloj.ahora(),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertDoesNotThrow(() -> funciones.programar(1, 1, reloj.ahora().plusMinutes(1),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    // El horizonte se mide en días: el mismo día del año que viene entra a cualquier hora, el siguiente no.
    @Test
    void noSeProgramaUnaFuncionAMasDeUnAnio() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2027, 8, 15, 20, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertEquals("La función tiene que empezar dentro del próximo año", error.getMessage());
        assertDoesNotThrow(() -> funciones.programar(1, 1, LocalDateTime.of(2027, 8, 14, 23, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    // R20 mira el alta, no el historial (R12): la que ya pasó sigue ahí.
    @Test
    void unaFuncionQueQuedoEnElPasadoSigueListada() {
        reloj.mover(LocalDateTime.of(2026, 8, 25, 10, 0));

        assertEquals(1, funciones.listar().size());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin criterios devuelve todo,                                     ,   ,  ,           ,           3
            las dos de Interstellar,                                         1,  ,  ,           ,           2
            las dos de la sala 1,                                            ,   1, ,           ,           2
            película y sala combinadas,                                      1,  1, ,           ,           1
            el rango incluye los dos extremos,                               ,   ,  2026-08-20, 2026-08-22, 3
            un solo día: desde y hasta iguales,                              ,   ,  2026-08-22, 2026-08-22, 1
            solo desde: de ahí en adelante,                                  ,   ,  2026-08-21, ,           1
            solo hasta: todo lo anterior,                                    ,   ,  ,           2026-08-21, 2
            un rango sin funciones da vacío,                                 ,   ,  2027-01-01, 2027-12-31, 0
            'una película que no existe no es un error, es cero resultados', 99, ,  ,           ,           0
            """)
    void buscarFiltraPorPeliculaSalaYRangoDeFechas(String caso, Integer pelicula, Integer sala,
            LocalDate desde, LocalDate hasta, int esperadas) {
        cargarMasFunciones();

        assertEquals(esperadas, funciones.buscar(pelicula, sala, new Periodo(desde, hasta)).size(), caso);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            empieza mientras corre otra,                    21:00
            pegada al final de la anterior por la limpieza, 22:00
            """)
    void rechazaFuncionQueChocaConLaDeLas20(String caso, LocalTime inicio) {
        assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDate.of(2026, 8, 20).atTime(inicio),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(1, funciones.listar().size());
    }

    // R3 se busca por rango y no en toda la historia de la sala: el rango tiene que alcanzar a
    // una función que empezó el día anterior y todavía se está proyectando.
    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreUnaLargaDelDiaAnterior() {
        cartelera.agregar("Satantango", 432, List.of(Genero.DRAMA), Clasificacion.MAS_16);
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 25, 22, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));

        assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 26, 4, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void elMensajeExplicaQueElChoqueEsPorLaLimpieza() {
        Rechazo e = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 5),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertTrue(e.getMessage().contains("limpieza"), e.getMessage());
        assertTrue(e.getMessage().contains("22:15"), e.getMessage());
    }

    @Test
    void aceptaFuncionCuandoYaTerminoLaLimpieza() {
        assertDoesNotThrow(
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 15),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(2, funciones.listar().size());
    }

    @Test
    void sinLimpiezaLasFuncionesSePuedenEncadenar() {
        salas.agregar("Sala sin corte", TipoSala.DOS_D, List.of(10, 10), Map.of(), 0);
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void rechazaSalaConLimpiezaNegativa() {
        assertThrows(Rechazo.class,
                () -> salas.agregar("Sala rota", TipoSala.DOS_D, List.of(10, 10), Map.of(), -5));
    }

    @Test
    void laLimpiezaNoAfectaALasOtrasSalas() {
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(6, 8), Map.of(), 30);

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void elMismoHorarioEnOtraSalaNoSePisa() {
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(6, 8));
        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(4500)));
    }

    @Test
    void noSePuedeProgramarUnaPeliculaPendienteDeRevision() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));

        Rechazo e = assertThrows(Rechazo.class,
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertEquals("La película Dune todavía no está confirmada: revisala antes de programarla", e.getMessage());
    }

    // La descartada no está en el buzón: pedirle que la revise la mandaría a buscar algo que no va a encontrar.
    @Test
    void unaPeliculaDescartadaDiceQueEstaDescartadaYNoQueFaltaRevisarla() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));
        revision.descartar(importada.getId());

        Rechazo e = assertThrows(Rechazo.class,
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertEquals("La película Dune está descartada: no se puede programar", e.getMessage());
    }

    @Test
    void unaVezConfirmadaSeProgramaNormal() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));
        revision.confirmar(importada.getId());

        assertDoesNotThrow(
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void rechazaPeliculaInexistente() {
        assertThrows(Rechazo.class,
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void noSeBorraUnaFuncionConReservas() {
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
        reservas.reservar(1, 1, generales("A1"), null);

        assertThrows(Rechazo.class, () -> funciones.eliminar(1));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void unaFuncionSinReservasSeBorra() {
        funciones.eliminar(1);
        assertEquals(0, funciones.listar().size());
    }

    @Test
    void noSeBorraLaSalaNiLaPeliculaConFuncionesProgramadas() {
        assertThrows(Rechazo.class, () -> salas.eliminar(1));
        assertThrows(Rechazo.class, () -> cartelera.eliminar(1));

        funciones.eliminar(1);
        assertDoesNotThrow(() -> cartelera.eliminar(1));
        assertDoesNotThrow(() -> salas.eliminar(1));
    }


    private static final int DURACION = 136;

    private Funcion funcionDeLas20() {
        return new Funcion(null, new Sala("Sala 1", TipoSala.DOS_D, 15), LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    @Test
    void unaFuncionNoEmpezoAntesDeSuHorario() {
        Funcion funcion = funcionDeLas20();

        assertFalse(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 19, 59)));
        assertTrue(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 20, 0)), "el minuto exacto ya cuenta");
        assertTrue(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 20, 1)));
    }

    @Test
    void elFinSaleDeLaDuracionDeLaPelicula() {
        assertEquals(LocalDateTime.of(2026, 8, 20, 22, 16), funcionDeLas20().getFin(DURACION));
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
