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

import ar.uade.cine.interfaces.ClienteDAO;
import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.Reserva;
import ar.uade.cine.interfaces.ReservaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.EstadoReserva;
import ar.uade.cine.modelo.Genero;
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
 * Reglas R4 (cupo), R5 (pagar) y R6 (cancelar libera lugares).
 * Usa ReservaDAOTxt sobre un directorio temporal, así de paso se prueba el DAO de archivo.
 */
class GestorReservasTest {

    @TempDir
    Path tempDir;

    private GestorReservas reservas;
    private Path directorioTickets;

    /** Sala de 10 lugares, una función, un cliente. */
    @BeforeEach
    void prepararEscenario() {
        PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
        SalaDAO salaDAO = new SalaDAOMemoria();
        FuncionDAO funcionDAO = new FuncionDAOMemoria();
        ClienteDAO clienteDAO = new ClienteDAOMemoria();
        ReservaDAO reservaDAO = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        directorioTickets = tempDir.resolve("tickets");

        new GestorCartelera(peliculaDAO).agregar("Matrix", 136, List.of(Genero.ACCION));
        new GestorSalas(salaDAO).agregar("Sala 1", 10);
        new GestorFunciones(funcionDAO, peliculaDAO, salaDAO)
                .programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0), 5000);
        new GestorClientes(clienteDAO).registrar("Andrei", "andrei@uade.edu.ar");

        reservas = new GestorReservas(reservaDAO, funcionDAO, salaDAO, clienteDAO, peliculaDAO,
                new GeneradorTicketTxt(directorioTickets));
    }

    @Test
    void reservarDescuentaLugares() {
        reservas.reservar(1, 1, 4);
        assertEquals(6, reservas.lugaresLibres(1));
    }

    @Test
    void rechazaReservaSinCupo() {
        reservas.reservar(1, 1, 8);
        assertThrows(IllegalArgumentException.class, () -> reservas.reservar(1, 1, 3));
    }

    @Test
    void cancelarLiberaLosLugares() {
        Reserva reserva = reservas.reservar(1, 1, 6);
        reservas.cancelar(reserva.getId());
        assertEquals(10, reservas.lugaresLibres(1));
    }

    @Test
    void noSePuedePagarDosVeces() {
        Reserva reserva = reservas.reservar(1, 1, 2);
        reservas.pagar(reserva.getId());
        assertEquals(EstadoReserva.PAGADA, reservas.buscar(reserva.getId()).orElseThrow().getEstado());
        assertThrows(IllegalArgumentException.class, () -> reservas.pagar(reserva.getId()));
    }

    @Test
    void emiteElTicketEnUnTxt() throws IOException {
        Reserva reserva = reservas.reservar(1, 1, 2);

        Path ticket = directorioTickets.resolve("ticket-" + reserva.getId() + ".txt");
        assertTrue(Files.exists(ticket), "no se generó el ticket");

        String contenido = Files.readString(ticket);
        assertTrue(contenido.contains("Matrix"), "el ticket no menciona la película");
        assertTrue(contenido.contains("Sala 1"), "el ticket no menciona la sala");
        assertTrue(contenido.contains("Andrei"), "el ticket no menciona al cliente");
        assertTrue(contenido.contains("20/08/2026 20:00"), "el ticket no tiene la fecha de la función");
        assertTrue(contenido.contains("10000"), "el total deberia ser 2 x 5000");
    }

    @Test
    void elDaoDeTextoPersisteEntreLecturas() {
        reservas.reservar(1, 1, 3);

        ReservaDAO otraInstancia = new ReservaDAOTxt(tempDir.resolve("reservas.txt"));
        assertEquals(1, otraInstancia.listar().size());
        assertEquals(3, otraInstancia.buscarPorId(1).orElseThrow().getCantidadEntradas());
    }
}
