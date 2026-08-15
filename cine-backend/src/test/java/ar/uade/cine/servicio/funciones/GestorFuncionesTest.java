package ar.uade.cine.servicio.funciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.infraestructura.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.BloqueoButacasMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import ar.uade.cine.servicio.cartelera.DatosPelicula;
import ar.uade.cine.servicio.cartelera.GestorCartelera;
import ar.uade.cine.servicio.programaciones.GestorProgramaciones;
import ar.uade.cine.servicio.salas.GestorSalas;
import ar.uade.cine.servicio.usuarios.GestorClientes;
import ar.uade.cine.servicio.ventas.CalculadoraPrecio;
import ar.uade.cine.servicio.ventas.GestorReservas;
import ar.uade.cine.servicio.ventas.Ocupacion;
import ar.uade.cine.dominio.dinero.Dinero;
import ar.uade.cine.servicio.cartelera.GestorRevisionCartelera;

/** R3: una sala no puede tener dos funciones superpuestas. R12: no se borra lo que está en uso. */
class GestorFuncionesTest {

    @TempDir
    Path tempDir;

    private final PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
    private final SalaDAO salaDAO = new SalaDAOMemoria();
    private final AsientoDAO asientoDAO = new AsientoDAOMemoria();
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();
    private final ClienteDAO clienteDAO = new ClienteDAOMemoria();

    private ReservaDAO reservaDAO;
    private GestorFunciones funciones;
    private GestorSalas salas;
    private GestorCartelera cartelera;
    private GestorRevisionCartelera revision;

    /** Película de 120 minutos en la sala 1, con una función a las 20:00. */
    @BeforeEach
    void prepararCartelera() {
        reservaDAO = new ReservaDAOMemoria();
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        cartelera = new GestorCartelera(peliculaDAO, funcionDAO, new GestorProgramaciones(
                new ProgramacionDAOMemoria(), funcionDAO, funciones));
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        revision = new GestorRevisionCartelera(peliculaDAO, funcionDAO, cartelera);
        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
    }

    // ---------- el buscador de la cartelera programada ----------

    /**
     * Segunda película y segunda sala, más funciones repartidas en tres días. Sin este
     * escenario cualquier filtro devolvería todo y los tests pasarían sin probar nada.
     */
    private void cargarMasFunciones() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(10, 10));
        // Interstellar en la sala 2, el mismo día que la del setup.
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
        // Matrix en la sala 1, dos días después.
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 22, 18, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    @Test
    void buscarSinCriteriosDevuelveTodo() {
        cargarMasFunciones();

        assertEquals(3, funciones.buscar(null, null, null, null).size());
    }

    @Test
    void buscarPorPeliculaYPorSala() {
        cargarMasFunciones();

        assertEquals(2, funciones.buscar(1, null, null, null).size(), "las dos de Interstellar");
        assertEquals(2, funciones.buscar(null, 1, null, null).size(), "las dos de la sala 1");
        // Cruzar los dos criterios deja una sola: es un Y, no un O.
        assertEquals(1, funciones.buscar(1, 1, null, null).size());
    }

    /**
     * El rango incluye los dos extremos. Quien filtra «del 20 al 22» espera ver el 22:
     * un rango semiabierto acá sería una sorpresa, no una convención.
     */
    @Test
    void elRangoDeFechasIncluyeLosDosExtremos() {
        cargarMasFunciones();

        assertEquals(3, funciones.buscar(null, null,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)).size());
        assertEquals(1, funciones.buscar(null, null,
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 22)).size(),
                "un solo día: desde y hasta iguales");
    }

    @Test
    void elRangoSeAbreDeUnLadoODelOtro() {
        cargarMasFunciones();

        assertEquals(1, funciones.buscar(null, null, LocalDate.of(2026, 8, 21), null).size(),
                "solo desde: de ahí en adelante");
        assertEquals(2, funciones.buscar(null, null, null, LocalDate.of(2026, 8, 21)).size(),
                "solo hasta: todo lo anterior");
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacioYNoFalla() {
        cargarMasFunciones();

        assertTrue(funciones.buscar(null, null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)).isEmpty());
        assertTrue(funciones.buscar(99, null, null, null).isEmpty(),
                "una película que no existe no es un error, es cero resultados");
    }

    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreOtra() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 21, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(1, funciones.listar().size());
    }

    /**
     * La función del setup termina 22:00 y la Sala 1 se limpia en 15 minutos, así que
     * pegar una a las 22:00 ya no alcanza: la sala está ocupada limpiándose.
     */
    @Test
    void rechazaFuncionPegadaAlFinalDeLaAnteriorPorLaLimpieza() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(1, funciones.listar().size());
    }

    /**
     * El mensaje tiene que decir que el problema es la limpieza y hasta cuándo dura. Sin
     * eso, el encargado ve "la sala está ocupada" a una hora en la que la cartelera no
     * muestra nada y lo lee como un error del sistema.
     */
    @Test
    void elMensajeExplicaQueElChoqueEsPorLaLimpieza() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
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

    /** Cero es válido: una sala que no necesita corte encadena funciones como antes. */
    @Test
    void sinLimpiezaLasFuncionesSePuedenEncadenar() {
        salas.agregar("Sala sin corte", TipoSala.DOS_D, List.of(10, 10), Map.of(), 0);
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    /**
     * Una limpieza negativa adelantaría el permiso para la función siguiente y la dejaría
     * empezar antes de que termine la anterior: es un dato mal cargado, no "sin limpieza".
     */
    @Test
    void rechazaSalaConLimpiezaNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> salas.agregar("Sala rota", TipoSala.DOS_D, List.of(10, 10), Map.of(), -5));
    }

    /** La limpieza es de la sala: la de al lado sigue libre a la misma hora. */
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

    /**
     * Sin esto el buzón de revisión no serviría de nada: bastaría con programar desde ahí
     * para meter en la cartelera del cine algo que nunca nadie aprobó.
     */
    @Test
    void noSePuedeProgramarUnaPeliculaPendienteDeRevision() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertTrue(e.getMessage().contains("confirmada"), e.getMessage());
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
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    /** R12: sin esto, borrar la función deja las reservas apuntando a la nada. */
    @Test
    void noSeBorraUnaFuncionConReservas() {
        GestorReservas reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO,
                clienteDAO, peliculaDAO, new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new CalculadoraPrecio(),
                new Ocupacion(reservaDAO, funcionDAO, asientoDAO, new BloqueoButacasMemoria()));
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria()).registrar("Andrei", "andrei@uade.edu.ar");
        reservas.reservar(1, 1, generales("A1"));

        assertThrows(IllegalArgumentException.class, () -> funciones.eliminar(1));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void unaFuncionSinReservasSeBorra() {
        funciones.eliminar(1);
        assertEquals(0, funciones.listar().size());
    }

    /** R12: la sala y la película tampoco se borran si tienen funciones programadas. */
    @Test
    void noSeBorraLaSalaNiLaPeliculaConFuncionesProgramadas() {
        assertThrows(IllegalArgumentException.class, () -> salas.eliminar(1));
        assertThrows(IllegalArgumentException.class, () -> cartelera.eliminar(1));

        funciones.eliminar(1);
        assertDoesNotThrow(() -> cartelera.eliminar(1));
        assertDoesNotThrow(() -> salas.eliminar(1));
    }


    // ---------- el paso del tiempo ----------

    /** Matrix dura 136 minutos: una función a las 20:00 termina 22:16. */
    private static final int DURACION = 136;

    private Funcion funcionDeLas20() {
        return new FuncionImpl(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
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
    void estaEnCursoEntreElInicioYElFin() {
        Funcion funcion = funcionDeLas20();

        assertFalse(funcion.estaEnCurso(LocalDateTime.of(2026, 8, 20, 19, 59), DURACION));
        assertTrue(funcion.estaEnCurso(LocalDateTime.of(2026, 8, 20, 21, 0), DURACION));
        assertFalse(funcion.estaEnCurso(LocalDateTime.of(2026, 8, 20, 23, 0), DURACION),
                "a las 23:00 ya termino: empezo 20:00 y dura 2h16");
    }

    @Test
    void elFinSaleDeLaDuracionDeLaPelicula() {
        assertEquals(LocalDateTime.of(2026, 8, 20, 22, 16), funcionDeLas20().getFin(DURACION));
    }

    /** El borde: en el minuto exacto del final todavía no terminó. */
    @Test
    void enElMinutoDelFinTodaviaNoTermino() {
        Funcion funcion = funcionDeLas20();

        assertFalse(funcion.yaTermino(LocalDateTime.of(2026, 8, 20, 22, 16), DURACION));
        assertTrue(funcion.yaTermino(LocalDateTime.of(2026, 8, 20, 22, 17), DURACION));
    }

    /** Butacas todas con tarifa general, que es el caso base de casi todas las pruebas. */
    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
