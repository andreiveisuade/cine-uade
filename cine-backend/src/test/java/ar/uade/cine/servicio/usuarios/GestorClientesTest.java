package ar.uade.cine.servicio.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.dominio.usuarios.Cliente;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;

/**
 * El cliente compra sin registrarse: se identifica con su email y, si es la primera vez,
 * se lo da de alta en el momento. Esa regla es del gestor, así que reservar por consola y
 * reservar por la web tienen que resolverla igual.
 */
class GestorClientesTest {

    @TempDir
    Path tempDir;

    private GestorClientes gestor;

    /** ReservaDAO y CompraCandyDAO solo hacen falta para la baja, que valida el historial. */
    @BeforeEach
    void prepararEscenario() {
        gestor = new GestorClientes(new ClienteDAOMemoria(),
                new ReservaDAOMemoria(), new CompraCandyDAOMemoria());
    }

    @Test
    void identificarDaDeAltaAlQueCompraPorPrimeraVez() {
        Cliente cliente = gestor.identificar("Andrei", "andrei@uade.edu.ar");

        assertTrue(cliente.getId() > 0);
        assertEquals(1, gestor.listar().size());
        assertEquals("andrei@uade.edu.ar", cliente.getEmail());
    }

    /** La segunda compra tiene que caer sobre el mismo cliente, no crear otro. */
    @Test
    void identificarDosVecesDevuelveElMismoCliente() {
        Cliente primera = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        Cliente segunda = gestor.identificar("Andrei", "andrei@uade.edu.ar");

        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, gestor.listar().size());
    }

    /**
     * El email que se busca y el que se guarda tienen que ser el mismo. Sin normalizar,
     * la compra con un espacio de más daría de alta un cliente repetido y le partiría el
     * historial en dos.
     */
    @Test
    void identificarIgnoraLosEspaciosDeMasEnElEmail() {
        Cliente primera = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        Cliente conEspacios = gestor.identificar("Andrei", "  andrei@uade.edu.ar  ");

        assertEquals(primera.getId(), conEspacios.getId());
        assertEquals(1, gestor.listar().size());
    }

    /** Identificar no relaja las validaciones del alta: el alta sigue siendo un alta. */
    @Test
    void identificarRechazaUnEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> gestor.identificar("Andrei", "sin-arroba"));
        assertThrows(IllegalArgumentException.class, () -> gestor.identificar("", "nuevo@uade.edu.ar"));
    }

    @Test
    void noSeRegistraDosVecesElMismoEmail() {
        gestor.registrar("Andrei", "andrei@uade.edu.ar");

        assertThrows(IllegalArgumentException.class,
                () -> gestor.registrar("Otro", "andrei@uade.edu.ar"));
    }
}
