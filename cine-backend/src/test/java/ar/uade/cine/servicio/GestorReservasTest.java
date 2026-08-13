package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.MedioPago;
import ar.uade.cine.dominio.ventas.Reserva;
import ar.uade.cine.persistencia.AsientoDAO;
import ar.uade.cine.persistencia.ClienteDAO;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.ReservaDAO;
import ar.uade.cine.persistencia.SalaDAO;
import ar.uade.cine.persistencia.archivo.GeneradorTicketTxt;
import ar.uade.cine.persistencia.archivo.ReservaDAOTxt;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;

/**
 * Reglas R4 (butaca libre), R6 (cancelar libera las butacas) y R13 (no se cancela una
 * reserva ya cobrada). Usa ReservaDAOTxt sobre un directorio temporal, así de paso se
 * prueba el DAO de archivo.
 */
class GestorReservasTest {

    @TempDir
    Path tempDir;

    private GestorReservas reservas;
    private GestorSalas salas;
    private GestorFunciones funciones;
    private GestorPagos pagos;
    private Path directorioTickets;

    /** Sala de 2 filas x 5 butacas (A1..A5, B1..B5), una función a $5000, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        AsientoDAO asientoDAO = new AsientoDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        ReservaDAO reservaDAO = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        directorioTickets = tempDir.resolve("tickets");

        new GestorCartelera(peliculaDAO, funcionDAO)
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas = new GestorSalas(salaDAO, asientoDAO, funcionDAO);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO, reservaDAO);
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
        new GestorClientes(clienteDAO, reservaDAO, new CompraCandyDAOMemoria()).registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(directorioTickets));
        pagos = new GestorPagos(new PagoDAOMemoria(), reservaDAO);
    }

    @Test
    void reservarOcupaSoloLasButacasElegidas() {
        reservas.reservar(1, 1, List.of("A1", "A2"));

        assertEquals(8, reservas.lugaresLibres(1));
        assertTrue(reservas.asientosLibres(1).stream().noneMatch(a -> a.getCodigo().equals("A1")));
        assertTrue(reservas.asientosLibres(1).stream().anyMatch(a -> a.getCodigo().equals("A3")));
    }

    @Test
    void rechazaButacaYaOcupada() {
        reservas.reservar(1, 1, List.of("B3"));
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, List.of("B3")));
    }

    @Test
    void rechazaButacaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, List.of("Z9")));
    }

    @Test
    void rechazaLaMismaButacaDosVecesEnUnaReserva() {
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, List.of("A1", "A1")));
    }

    @Test
    void cancelarLiberaLasButacas() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1", "A2", "A3"));
        reservas.cancelar(reserva.getId());

        assertEquals(10, reservas.lugaresLibres(1));
        assertTrue(reservas.asientosLibres(1).stream().anyMatch(a -> a.getCodigo().equals("A1")));
    }

    /** R13: si se cancelara, el pago seguiría contando en el arqueo del día. */
    @Test
    void noSePuedeCancelarUnaReservaYaCobrada() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        pagos.cobrar(reserva.getId(), MedioPago.EFECTIVO, "");

        assertThrows(IllegalArgumentException.class, () -> reservas.cancelar(reserva.getId()));
        assertEquals(9, reservas.lugaresLibres(1), "la butaca cobrada sigue ocupada");
    }

    @Test
    void noSeCancelaDosVecesLaMismaReserva() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));
        reservas.cancelar(reserva.getId());

        assertThrows(IllegalArgumentException.class, () -> reservas.cancelar(reserva.getId()));
    }

    @Test
    void unaButacaFueraDeServicioNoSePuedeReservar() {
        salas.marcarFueraDeServicio(1, "A3");

        assertEquals(9, reservas.lugaresLibres(1));
        assertTrue(reservas.asientosLibres(1).stream().noneMatch(a -> a.getCodigo().equals("A3")));
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, List.of("A3")));
    }

    @Test
    void reponerLaButacaLaVuelveAHabilitar() {
        salas.marcarFueraDeServicio(1, "A3");
        salas.reponer(1, "A3");

        assertEquals(10, reservas.lugaresLibres(1));
        assertEquals(1, reservas.reservar(1, 1, List.of("A3")).getCantidadEntradas());
    }

    @Test
    void emiteElTicketConLasButacas() throws IOException {
        Reserva reserva = reservas.reservar(1, 1, List.of("B4", "B5"));

        Path ticket = directorioTickets.resolve("ticket-" + reserva.getId() + ".txt");
        assertTrue(Files.exists(ticket), "no se generó el ticket");

        String contenido = Files.readString(ticket);
        assertTrue(contenido.contains("Matrix"), "el ticket no menciona la película");
        assertTrue(contenido.contains("Sala 1"), "el ticket no menciona la sala");
        assertTrue(contenido.contains("Andrei"), "el ticket no menciona al cliente");
        assertTrue(contenido.contains("Butaca B4"), "el ticket no lista la butaca B4");
        assertTrue(contenido.contains("Butaca B5"), "el ticket no lista la butaca B5");
        assertTrue(contenido.contains("10000"), "el total deberia ser 2 x 5000");
    }

    @Test
    void elPrecioDependeDelTipoDeSalaYDeButaca() {
        // Sala 2 es IMAX (x1.6) y su butaca A1 es VIP (x1.5); base 5000 => 12000
        salas.agregar("Sala 2", TipoSala.IMAX, List.of(4), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 21, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, 5000);

        Reserva vip = reservas.reservar(2, 1, List.of("A1"));
        assertEquals(12000.0, vip.getTotal(), 0.001);

        Reserva estandar = reservas.reservar(2, 1, List.of("A2"));
        assertEquals(8000.0, estandar.getTotal(), 0.001);
    }

    /**
     * El recargo premium se cobra una sola vez. Antes TipoSala tenía VIP además de
     * TipoAsiento, y una butaca VIP en sala VIP pagaba 2.0 x 1.5 = 3 veces la base.
     */
    @Test
    void elRecargoPremiumSeAplicaUnaSolaVez() {
        salas.agregar("Sala VIP", TipoSala.IMAX, List.of(2), Map.of("A1", TipoAsiento.VIP));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 22, 20, 0),
                Version.DOBLADA, Proyeccion.DOS_D, 5000);

        // 5000 x 1.6 (IMAX) x 1.5 (butaca VIP), y nada más
        assertEquals(12000.0, reservas.reservar(2, 1, List.of("A1")).getTotal(), 0.001);
    }

    /** Sin redondeo, 5250.50 x 1.3 da 6825.650000000001 y eso llega al ticket. */
    @Test
    void elPrecioSeRedondeaADosDecimales() {
        salas.agregar("Sala 3D", TipoSala.TRES_D, List.of(3));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 23, 20, 0),
                Version.DOBLADA, Proyeccion.TRES_D, 5250.50);

        assertEquals(6825.65, reservas.reservar(2, 1, List.of("A1")).getTotal());
    }

    @Test
    void guardaCuandoSeHizoLaReserva() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A1"));

        assertEquals(LocalDate.now(), reserva.getCreadaEn().toLocalDate());
    }

    @Test
    void noSePuedeProgramar3DEnUnaSalaQueNoLoSoporta() {
        salas.agregar("Sala 2D", TipoSala.DOS_D, List.of(4));
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 24, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, 5000));
    }

    @Test
    void elDaoDeTextoPersisteLasButacasYLaFecha() {
        reservas.reservar(1, 1, List.of("A1", "B2"));

        ReservaDAO otraInstancia = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        Reserva leida = otraInstancia.buscarPorId(1).orElseThrow();
        assertEquals(2, leida.getCantidadEntradas());
        assertEquals("A1", leida.getEntradas().get(0).codigoAsiento());
        assertEquals(LocalDate.now(), leida.getCreadaEn().toLocalDate());
    }
}
