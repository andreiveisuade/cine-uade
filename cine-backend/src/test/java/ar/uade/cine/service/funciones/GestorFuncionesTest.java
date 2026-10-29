package ar.uade.cine.service.funciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.tiempo.Periodo;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.repository.cartelera.PeliculaRepository;
import ar.uade.cine.repository.funciones.FuncionRepository;
import ar.uade.cine.repository.salas.SalaRepository;
import ar.uade.cine.repository.ventas.ReservaRepository;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.GestorReservas;

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
    @Autowired
    private FuncionRepository funcionRepository;
    @Autowired
    private PeliculaRepository peliculaRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void prepararCartelera() {
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    private void cargarMasFunciones() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 22, 18, 0),
                new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    // R19: la cartelera ofrece solo lo que todavía se puede comprar; la que ya empezó sale de la lista.
    @Test
    void laCarteleraDeUnaPeliculaNoOfreceFuncionesQueYaEmpezaron() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        reloj.mover(LocalDateTime.of(2026, 8, 20, 20, 0));

        List<LocalDateTime> inicios = funciones.proximasDe(1).stream().map(Funcion::getInicio).toList();

        assertEquals(List.of(LocalDateTime.of(2026, 8, 21, 20, 0)), inicios);
    }

    // Dos altas iguales a la vez pasan R3 juntas: el UNIQUE frena la segunda y el cine responde 409, no 500.
    @Test
    void laMismaFuncionDadaDeAltaDosVecesALaVezEsUnConflicto() {
        GestorFunciones otroPedido = new GestorFuncionesQueNoVeLaCarrera(funcionRepository, peliculaRepository,
                salaRepository, reservaRepository, reloj);

        Rechazo error = assertThrows(ConflictoDeNegocio.class,
                () -> otroPedido.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertEquals("Ya hay una función en esa sala a esa hora", error.getMessage());
        assertEquals(1, funcionRepository.findAll().size());
    }

    // R20
    @Test
    void noSeProgramaUnaFuncionEnElPasado() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, reloj.ahora().minusMinutes(1),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertEquals("La función no puede empezar en el pasado", error.getMessage());
        assertEquals(1, funciones.listar().size(), "solo la del arranque");
    }

    // R20 con el corte de R19: la que empieza en este instante ya empezó.
    @Test
    void ahoraMismoYaCuentaComoPasadoYUnMinutoDespuesNo() {
        rechaza("La función no puede empezar en el pasado",
                () -> funciones.programar(1, 1, reloj.ahora(),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertDoesNotThrow(() -> funciones.programar(1, 1, reloj.ahora().plusMinutes(1),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    // El horizonte se mide en días: el mismo día del año que viene entra a cualquier hora, el siguiente no.
    @Test
    void noSeProgramaUnaFuncionAMasDeUnAnio() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2027, 8, 15, 20, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertEquals("La función tiene que empezar dentro del próximo año", error.getMessage());
        assertDoesNotThrow(() -> funciones.programar(1, 1, LocalDateTime.of(2027, 8, 14, 23, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
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
            empieza mientras corre otra,                    21:00, La sala ya tiene una función en ese horario
            pegada al final de la anterior por la limpieza, 22:00, 'La sala necesita 15 minutos de limpieza: la función anterior termina 22:00 y hasta 22:15 no se puede empezar'
            """)
    void rechazaFuncionQueChocaConLaDeLas20(String caso, LocalTime inicio, String mensaje) {
        rechaza(mensaje,
                () -> funciones.programar(1, 1, LocalDate.of(2026, 8, 20).atTime(inicio),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
        assertEquals(1, funciones.listar().size());
    }

    // R3 se busca por rango: tiene que alcanzar a la función que empezó ayer y sigue proyectándose.
    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreUnaLargaDelDiaAnterior() {
        cartelera.agregar("Satantango", 432, List.of(Genero.DRAMA), Clasificacion.MAS_16);
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 25, 22, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        rechaza("La sala ya tiene una función en ese horario",
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 26, 4, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void elMensajeExplicaQueElChoqueEsPorLaLimpieza() {
        Rechazo e = assertThrows(Rechazo.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 5),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertTrue(e.getMessage().contains("limpieza"), e.getMessage());
        assertTrue(e.getMessage().contains("22:15"), e.getMessage());
    }

    @Test
    void aceptaFuncionCuandoYaTerminoLaLimpieza() {
        assertDoesNotThrow(
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 15),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
        assertEquals(2, funciones.listar().size());
    }

    @Test
    void sinLimpiezaLasFuncionesSePuedenEncadenar() {
        salas.agregar("Sala sin corte", TipoSala.DOS_D, List.of(10, 10), Map.of(), 0);
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void rechazaSalaConLimpiezaNegativa() {
        rechaza("Los minutos de limpieza no pueden ser negativos",
                () -> salas.agregar("Sala rota", TipoSala.DOS_D, List.of(10, 10), Map.of(), -5));
    }

    @Test
    void laLimpiezaNoAfectaALasOtrasSalas() {
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(6, 8), Map.of(), 30);

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void elMismoHorarioEnOtraSalaNoSePisa() {
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(6, 8));
        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                        new FormatoDeFuncion(Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(4500))));
    }

    @Test
    void noSePuedeProgramarUnaPeliculaPendienteDeRevision() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));

        Rechazo e = assertThrows(Rechazo.class,
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500))));

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
                        new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500))));

        assertEquals("La película Dune está descartada: no se puede programar", e.getMessage());
    }

    @Test
    void unaVezConfirmadaSeProgramaNormal() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));
        revision.confirmar(importada.getId());

        assertDoesNotThrow(
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void rechazaPeliculaInexistente() {
        rechaza("No existe la película 99",
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                        new FormatoDeFuncion(Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void noSeBorraUnaFuncionConReservas() {
        clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar");
        reservas.reservar(1, 1, generales("A1"), null);

        rechaza("La función 1 tiene reservas: no se puede eliminar", () -> funciones.eliminar(1));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void unaFuncionSinReservasSeBorra() {
        funciones.eliminar(1);
        assertEquals(0, funciones.listar().size());
    }

    @Test
    void noSeBorraLaSalaNiLaPeliculaConFuncionesProgramadas() {
        rechaza("La sala 1 tiene funciones programadas: primero hay que eliminarlas", () -> salas.eliminar(1));
        rechaza("La película Interstellar tiene funciones programadas: sacala de cartelera en vez de borrarla",
                () -> cartelera.eliminar(1));

        funciones.eliminar(1);
        assertDoesNotThrow(() -> cartelera.eliminar(1));
        assertDoesNotThrow(() -> salas.eliminar(1));
    }

    // R12 como el borrado: una reserva cancelada también es historial de la función.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            con una reserva activa,    false
            con una reserva cancelada, true
            """)
    void noSeEditaUnaFuncionConReservas(String caso, boolean cancelarla) {
        clientes.ingresarConGoogle("Andrei", "andrei@uade.edu.ar");
        int reserva = reservas.reservar(1, 1, generales("A1"), null).getId();
        if (cancelarla) {
            reservas.cancelar(reserva);
        }

        rechaza("La función 1 tiene reservas: no se puede editar", () -> funciones.reprogramar(1, 1, 1,
                LocalDateTime.of(2026, 8, 20, 21, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 0), funciones.obtener(1).getInicio());
    }

    @Test
    void editarCambiaTodoLoQueElAltaRecibe() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(10, 10));

        funciones.reprogramar(1, 2, 2, LocalDateTime.of(2026, 8, 21, 18, 30),
                new FormatoDeFuncion(Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(6000)));

        Funcion editada = funciones.obtener(1);
        assertEquals(2, editada.getPeliculaId());
        assertEquals(2, editada.getSalaId());
        assertEquals(LocalDateTime.of(2026, 8, 21, 18, 30), editada.getInicio());
        assertEquals(new FormatoDeFuncion(Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(6000)), editada.getFormato());
        assertEquals(1, funciones.listar().size(), "edita la misma, no crea otra");
    }

    // R3 sin la función misma: moverla media hora pisa su propio horario viejo, y eso no es un choque.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin cambiar el horario,     2026-08-20T20:00
            media hora más tarde,       2026-08-20T20:30
            media hora más temprano,    2026-08-20T19:30
            """)
    void editarNoChocaConElHorarioQueDeja(String caso, LocalDateTime inicio) {
        assertDoesNotThrow(() -> funciones.reprogramar(1, 1, 1, inicio,
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000))));

        assertEquals(inicio, funciones.obtener(1).getInicio());
    }

    // Mismas validaciones y mensajes que el alta, en el mismo orden; la otra función de la sala es la del 21 a las 20.
    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            película inexistente,          99, 1,  2026-08-20T20:00,    DOS_D,  No existe la película 99
            sala inexistente,              1,  99, 2026-08-20T20:00,    DOS_D,  No existe la sala 99
            R8: 3D en una sala 2D,         1,  1,  2026-08-20T20:00,    TRES_D, La sala Sala 1 no puede proyectar en 3D
            con segundos,                  1,  1,  2026-08-20T20:00:30, DOS_D,  La hora de la función tiene que ir sin segundos
            R20: en el pasado,             1,  1,  2026-08-14T09:00,    DOS_D,  La función no puede empezar en el pasado
            a más de un año,               1,  1,  2027-08-15T20:00,    DOS_D,  La función tiene que empezar dentro del próximo año
            R3: mientras corre otra,       1,  1,  2026-08-21T21:00,    DOS_D,  La sala ya tiene una función en ese horario
            R3: a la misma hora que otra,  1,  1,  2026-08-21T20:00,    DOS_D,  La sala ya tiene una función en ese horario
            R3: en la limpieza de otra,    1,  1,  2026-08-21T22:05,    DOS_D,  'La sala necesita 15 minutos de limpieza: la función anterior termina 22:00 y hasta 22:15 no se puede empezar'
            """)
    void editarValidaComoElAltaYSiRechazaNoCambiaNada(String caso, int pelicula, int sala, LocalDateTime inicio,
            Proyeccion proyeccion, String mensaje) {
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        rechaza(mensaje, () -> funciones.reprogramar(1, pelicula, sala, inicio,
                new FormatoDeFuncion(Version.SUBTITULADA, proyeccion, Dinero.de(4500))));
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 0), funciones.obtener(1).getInicio());
    }

    @Test
    void noSeEditaUnaFuncionQueNoExiste() {
        rechaza("No existe la función 99", () -> funciones.reprogramar(99, 1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    @Test
    void noSeEditaHaciaUnaPeliculaPendienteDeRevision() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));

        rechaza("La película Dune todavía no está confirmada: revisala antes de programarla",
                () -> funciones.reprogramar(1, importada.getId(), 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                        new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
    }

    // Dos pedidos a la vez pasan R3 juntos: el UNIQUE frena el segundo, también si es una edición.
    @Test
    void editarHaciaElHorarioQueOtroTomoALaVezEsUnConflicto() {
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        GestorFunciones otroPedido = new GestorFuncionesQueNoVeLaCarrera(funcionRepository, peliculaRepository,
                salaRepository, reservaRepository, reloj);

        rechaza("Ya hay una función en esa sala a esa hora", () -> otroPedido.reprogramar(1, 1, 1,
                LocalDateTime.of(2026, 8, 21, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500))));
        assertEquals(LocalDateTime.of(2026, 8, 20, 20, 0), funciones.obtener(1).getInicio());
    }


    private static final int DURACION = 136;

    private Funcion funcionDeLas20() {
        return new Funcion(null, new Sala("Sala 1", TipoSala.DOS_D, 15), LocalDateTime.of(2026, 8, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
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
