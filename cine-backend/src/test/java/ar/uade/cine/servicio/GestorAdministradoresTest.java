package ar.uade.cine.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.usuarios.AdministradorCine;
import ar.uade.cine.dominio.usuarios.Rol;
import ar.uade.cine.persistencia.memoria.AdministradorDAOMemoria;

class GestorAdministradoresTest {

    private GestorAdministradores administradores;

    @BeforeEach
    void registrarUno() {
        administradores = new GestorAdministradores(new AdministradorDAOMemoria());
        administradores.registrar("Encargado", "encargado@cine.com", "secreta123");
    }

    @Test
    void iniciaSesionConLasCredencialesCorrectas() {
        AdministradorCine admin = administradores.iniciarSesion("encargado@cine.com", "secreta123");

        assertEquals("Encargado", admin.getNombre());
        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
    }

    @Test
    void rechazaLaContrasenaEquivocada() {
        assertThrows(IllegalArgumentException.class,
                () -> administradores.iniciarSesion("encargado@cine.com", "otracosa"));
    }

    @Test
    void rechazaUnEmailQueNoExiste() {
        assertThrows(IllegalArgumentException.class,
                () -> administradores.iniciarSesion("nadie@cine.com", "secreta123"));
    }

    @Test
    void noGuardaLaContrasenaEnTextoPlano() {
        AdministradorCine admin = administradores.iniciarSesion("encargado@cine.com", "secreta123");
        assertNotEquals("secreta123", admin.getPasswordHash());
        assertEquals(64, admin.getPasswordHash().length(), "SHA-256 en hexa son 64 caracteres");
    }

    @Test
    void rechazaContrasenaCorta() {
        assertThrows(IllegalArgumentException.class,
                () -> administradores.registrar("Otro", "otro@cine.com", "123"));
    }

    @Test
    void rechazaEmailRepetido() {
        assertThrows(IllegalArgumentException.class,
                () -> administradores.registrar("Otro", "encargado@cine.com", "secreta123"));
    }
}
