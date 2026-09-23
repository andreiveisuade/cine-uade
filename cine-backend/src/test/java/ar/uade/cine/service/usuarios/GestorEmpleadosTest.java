package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.usuarios.Empleado;
import ar.uade.cine.model.usuarios.Rol;

class GestorEmpleadosTest extends PruebaDeIntegracion {

    @Autowired
    private GestorEmpleados empleados;

    @BeforeEach
    void registrarUno() {
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);
    }

    @Test
    void seEncuentraPorEmailConSuRol() {
        Empleado admin = empleados.buscarPorEmail("encargado@cine.com").orElseThrow();

        assertEquals("Encargado", admin.getNombre());
        assertEquals(Rol.ADMINISTRADOR, admin.getRol());
        assertTrue(empleados.buscarPorEmail("nadie@cine.com").isEmpty());
    }

    @Test
    void noGuardaLaContrasenaEnTextoPlano() {
        Empleado admin = empleados.buscarPorEmail("encargado@cine.com").orElseThrow();

        assertNotEquals("secreta123", admin.getPasswordHash());
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

    @Test
    void noSeRegistraUnClienteComoEmpleado() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Ana", "ana@mail.com", "secreta123", Rol.CLIENTE));
    }
}
