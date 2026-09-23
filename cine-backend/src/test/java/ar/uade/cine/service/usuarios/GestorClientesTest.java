package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.usuarios.Cliente;

/** El cliente compra sin registrarse: se identifica por email y se da de alta la primera vez. */
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

    @Test
    void identificarDosVecesDevuelveElMismoCliente() {
        Cliente primera = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        Cliente segunda = gestor.identificar("Andrei", "andrei@uade.edu.ar");

        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, gestor.listar().size());
    }

    /** Sin normalizar, un espacio de más duplicaría al cliente y partiría su historial. */
    @Test
    void identificarIgnoraLosEspaciosDeMasEnElEmail() {
        Cliente primera = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        Cliente conEspacios = gestor.identificar("Andrei", "  andrei@uade.edu.ar  ");

        assertEquals(primera.getId(), conEspacios.getId());
        assertEquals(1, gestor.listar().size());
    }

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
