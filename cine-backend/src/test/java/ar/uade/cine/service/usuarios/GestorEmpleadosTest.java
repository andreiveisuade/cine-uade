package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.memoria.EmpleadoDAOMemoria;

class GestorEmpleadosTest {

    private GestorEmpleados empleados;

    @BeforeEach
    void registrarUno() {
        empleados = new GestorEmpleados(new EmpleadoDAOMemoria());
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);
    }

    @Test
    void iniciaSesionConLasCredencialesCorrectas() {
        Empleado admin = empleados.iniciarSesion("encargado@cine.com", "secreta123");

        assertEquals("Encargado", admin.getNombre());
        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
    }

    @Test
    void rechazaLaContrasenaEquivocada() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.iniciarSesion("encargado@cine.com", "otracosa"));
    }

    @Test
    void rechazaUnEmailQueNoExiste() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.iniciarSesion("nadie@cine.com", "secreta123"));
    }

    @Test
    void noGuardaLaContrasenaEnTextoPlano() {
        Empleado admin = empleados.iniciarSesion("encargado@cine.com", "secreta123");
        assertNotEquals("secreta123", admin.getPasswordHash());
        assertEquals(64, admin.getPasswordHash().length(), "SHA-256 en hexa son 64 caracteres");
    }

    @Test
    void rechazaContrasenaCorta() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Otro", "otro@cine.com", "123", Rol.ADMINISTRADOR));
    }

    @Test
    void rechazaEmailRepetido() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Otro", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR));
    }

    /** El cliente no tiene contraseña: darlo de alta acá lo dejaría iniciar sesión. */
    @Test
    void noSeRegistraUnClienteComoEmpleado() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Ana", "ana@mail.com", "secreta123", Rol.CLIENTE));
    }

    @Test
    void elAcomodadorTambienIniciaSesionYConservaSuRol() {
        empleados.registrar("Puerta", "puerta@cine.com", "secreta123", Rol.ACOMODADOR);

        Empleado acomodador = empleados.iniciarSesion("puerta@cine.com", "secreta123");

        assertEquals(Rol.ACOMODADOR, acomodador.getRol());
        assertTrue(acomodador.getRol().esEmpleado());
    }
}
