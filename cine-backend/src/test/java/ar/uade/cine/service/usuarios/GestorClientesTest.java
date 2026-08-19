package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.usuarios.Cliente;

/**
 * El cliente compra sin registrarse: se identifica con su email y, si es la primera vez,
 * se lo da de alta en el momento. Esa regla es del gestor, así que reservar por consola y
 * reservar por la web tienen que resolverla igual.
 */
class GestorClientesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorClientes gestor;

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
