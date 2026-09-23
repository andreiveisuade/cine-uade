package ar.uade.cine.model.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class EmpleadoTest {

    private static void rechaza(String mensaje, Executable construir) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, construir).getMessage());
    }

    // La fórmula del discriminador lo releería como Cliente: no puede existir.
    @Test
    void unEmpleadoConRolClienteNoSeConstruye() {
        rechaza("El rol tiene que ser ADMINISTRADOR o ACOMODADOR",
                () -> new Empleado("Ana", "ana@cine.com", "{bcrypt}x", Rol.CLIENTE));
        rechaza("El rol tiene que ser ADMINISTRADOR o ACOMODADOR",
                () -> new Empleado("Ana", "ana@cine.com", "{bcrypt}x", null));
    }

    @Test
    void nombreYEmailLosValidaUsuarioParaLasDosClases() {
        rechaza("El nombre no puede estar vacío", () -> new Empleado(" ", "ana@cine.com", "{bcrypt}x", Rol.ACOMODADOR));
        rechaza("El email no es válido", () -> new Cliente("Ana", "ana.cine.com"));
    }

    @Test
    void losDosRolesDeEmpleadoSeConstruyen() {
        assertEquals(Rol.ADMINISTRADOR, new Empleado("Encargado", "e@cine.com", "{bcrypt}x", Rol.ADMINISTRADOR).getRol());
        assertEquals(Rol.ACOMODADOR, new Empleado("Portero", "p@cine.com", "{bcrypt}x", Rol.ACOMODADOR).getRol());
    }
}
