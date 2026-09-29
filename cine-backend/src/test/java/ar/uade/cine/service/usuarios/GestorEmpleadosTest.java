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
import ar.uade.cine.service.ConflictoDeNegocio;

class GestorEmpleadosTest extends PruebaDeIntegracion {

    @Autowired
    private GestorEmpleados empleados;

    @Autowired
    private GestorClientes clientes;

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
    void unEmpleadoNuevoQuedaEnBcrypt() {
        Empleado admin = empleados.buscarPorEmail("encargado@cine.com").orElseThrow();

        assertNotEquals("secreta123", admin.getPasswordHash());
        assertTrue(admin.getPasswordHash().startsWith("{bcrypt}$2a$"), admin.getPasswordHash());
    }

    @Test
    void rechazaContrasenaCorta() {
        assertEquals("La contraseña tiene que tener al menos 6 caracteres", assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Otro", "otro@cine.com", "123", Rol.ADMINISTRADOR)).getMessage());
    }

    @Test
    void rechazaEmailRepetido() {
        assertEquals("Ya existe un usuario con ese email", assertThrows(ConflictoDeNegocio.class,
                () -> empleados.registrar("Otro", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR))
                .getMessage());
    }

    // El simétrico del cliente con email de empleado: EmpleadoRepository no ve a los clientes y el
    // INSERT chocaba con el UNIQUE del email.
    @Test
    void elEmailDeUnClienteNoSeRegistraComoEmpleado() {
        clientes.registrar("Ana", "ana@mail.com");

        assertEquals("Ya existe un usuario con ese email", assertThrows(ConflictoDeNegocio.class,
                () -> empleados.registrar("Ana", "ana@mail.com", "secreta123", Rol.ACOMODADOR)).getMessage());
        assertEquals("Ya existe un usuario con ese email", assertThrows(ConflictoDeNegocio.class,
                () -> empleados.registrar("Ana", " ANA@mail.com", "secreta123", Rol.ACOMODADOR)).getMessage());
    }

    @Test
    void seEncuentraPorEmailSinDistinguirMayusculasNiEspacios() {
        assertTrue(empleados.buscarPorEmail(" Encargado@CINE.com ").isPresent());
    }

    @Test
    void noSeRegistraUnClienteComoEmpleado() {
        assertThrows(IllegalArgumentException.class,
                () -> empleados.registrar("Ana", "ana@mail.com", "secreta123", Rol.CLIENTE));
    }
}
