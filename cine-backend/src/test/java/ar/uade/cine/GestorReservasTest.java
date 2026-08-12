package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.interfaces.AsientoDAO;
import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.TipoSala;
import ar.uade.cine.persistencia.AsientoDAOMemoria;
import ar.uade.cine.persistencia.ClienteDAOMemoria;
import ar.uade.cine.persistencia.FuncionDAOMemoria;
import ar.uade.cine.persistencia.GeneradorTicketTxt;
import ar.uade.cine.persistencia.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.ReservaDAOTxt;
import ar.uade.cine.persistencia.SalaDAOMemoria;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorClientes;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorReservas;
import ar.uade.cine.servicio.GestorSalas;

/**
 * Reglas R4 (butaca libre), R5 (pagar) y R6 (cancelar libera las butacas).
 * Usa ReservaDAOTxt sobre un directorio temporal, así de paso se prueba el DAO de archivo.
 */
class GestorReservasTest {

    @TempDir
    Path tempDir;

    private GestorReservas reservas;
    private Path directorioTickets;

    /** Sala de 2 filas x 5 butacas (A1..A5, B1..B5), una función, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        AsientoDAO asientoDAO = new AsientoDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        ReservaDAO reservaDAO = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        directorioTickets = tempDir.resolve("tickets");

        new GestorCartelera(peliculaDAO).agregar("Matrix", 136, List.of(Genero.ACCION));
        new GestorSalas(salaDAO, asientoDAO).agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        new GestorFunciones(funcionDAO, peliculaDAO, salaDAO)
                .programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0), 5000);
        new GestorClientes(clienteDAO).registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, asientoDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(directorioTickets));
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

    @Test
    void noSePuedePagarDosVeces() {
        Reserva reserva = reservas.reservar(1, 1, List.of("A4"));
        reservas.pagar(reserva.getId());
        assertEquals(EstadoReserva.PAGADA, reservas.buscar(reserva.getId()).orElseThrow().getEstado());
        assertThrows(IllegalArgumentException.class, () -> reservas.pagar(reserva.getId()));
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
        assertTrue(contenido.contains("B4, B5"), "el ticket no lista las butacas");
        assertTrue(contenido.contains("10000"), "el total deberia ser 2 x 5000");
    }

    @Test
    void elDaoDeTextoPersisteLasButacas() {
        reservas.reservar(1, 1, List.of("A1", "B2"));

        ReservaDAO otraInstancia = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        Reserva leida = otraInstancia.buscarPorId(1).orElseThrow();
        assertEquals(2, leida.getCantidadEntradas());
        assertEquals("A1", leida.getEntradas().get(0).getCodigoAsiento());
    }
}
