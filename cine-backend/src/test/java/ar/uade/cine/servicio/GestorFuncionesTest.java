package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
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
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

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

    /** Película de 120 minutos en la sala 1, con una función a las 20:00. */
    @BeforeEach
    void prepararCartelera() {
        reservaDAO = new ReservaDAOMemoria();
        cartelera = new GestorCartelera(peliculaDAO, funcionDAO);
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 4500);
    }

    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreOtra() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 21, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, 4500));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void aceptaFuncionDespuesDeQueTerminaLaAnterior() {
        assertDoesNotThrow(
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, 4500));
        assertEquals(2, funciones.listar().size());
    }

    @Test
    void elMismoHorarioEnOtraSalaNoSePisa() {
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(6, 8));
        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, 4500));
    }

    @Test
    void rechazaPeliculaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, 4500));
    }

    /** R12: sin esto, borrar la función deja las reservas apuntando a la nada. */
    @Test
    void noSeBorraUnaFuncionConReservas() {
        GestorReservas reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO,
                clienteDAO, peliculaDAO, new GeneradorTicketTxt(tempDir.resolve("tickets")), new CalculadoraPrecio());
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
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
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
