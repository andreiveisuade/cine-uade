package ar.uade.cine.model.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.Rechazo;

class EmpleadoTest {

    private static void rechaza(String mensaje, Executable construir) {
        assertEquals(mensaje, assertThrows(Rechazo.class, construir).getMessage());
    }

    // La fórmula del discriminador lo releería como Cliente: no puede existir.
    @ParameterizedTest(name = "{0}")
    @EnumSource(names = "CLIENTE")
    @NullSource
    void unEmpleadoConRolClienteNoSeConstruye(Rol rol) {
        rechaza("El rol tiene que ser encargado o acomodador",
                () -> new Empleado("Ana", "ana@cine.com", "{bcrypt}x", rol));
    }

    @Test
    void nombreYEmailLosValidaUsuarioParaLasDosClases() {
        rechaza("Falta el nombre", () -> new Empleado(" ", "ana@cine.com", "{bcrypt}x", Rol.ACOMODADOR));
        rechaza("El email tiene que tener la forma usuario@dominio.com", () -> new Cliente("Ana", "ana.cine.com"));
    }

    @ParameterizedTest(name = "nombre [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void sinNombreFaltaElNombre(String nombre) {
        rechaza("Falta el nombre", () -> new Cliente(nombre, "ana@cine.com"));
    }

    // El VARCHAR(100) de la columna: pasado, el INSERT fallaba con un 500.
    @Test
    void unNombreDeMasDeCienCaracteresSeRechaza() {
        rechaza("El nombre no puede tener más de 100 caracteres", () -> new Cliente("x".repeat(101), "ana@cine.com"));
    }

    // Con varios datos mal sale uno solo, y siempre el mismo: nombre, email, rol, contraseña.
    @Test
    void conVariosDatosMalSaleElPrimeroEnOrden() {
        rechaza("Falta el nombre", () -> new Empleado(" ", "sin-arroba", null, Rol.CLIENTE));
        rechaza("El email tiene que tener la forma usuario@dominio.com",
                () -> new Empleado("Ana", "sin-arroba", null, Rol.CLIENTE));
        rechaza("El rol tiene que ser encargado o acomodador",
                () -> new Empleado("Ana", "ana@cine.com", null, Rol.CLIENTE));
        rechaza("Falta la contraseña", () -> new Empleado("Ana", "ana@cine.com", null, Rol.ACOMODADOR));
    }

    // Con NULL en la base, el login no tiene con qué comparar la clave.
    @ParameterizedTest(name = "hash [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void unEmpleadoNoSeQuedaSinContrasena(String hash) {
        Empleado empleado = new Empleado("Ana", "ana@cine.com", "{bcrypt}x", Rol.ACOMODADOR);

        rechaza("Falta la contraseña", () -> new Empleado("Ana", "ana@cine.com", hash, Rol.ACOMODADOR));
        rechaza("Falta la contraseña", () -> empleado.reemplazarPasswordHash(hash));
        assertEquals("{bcrypt}x", empleado.getPasswordHash());
    }

    // API.md promete emails sin distinguir mayúsculas: «BETO@x.com» y «beto@x.com» eran dos clientes.
    @Test
    void elEmailSeGuardaEnMinusculas() {
        assertEquals("beto@cine.com", new Cliente("Beto", "BETO@Cine.com").getEmail());
        assertEquals("beto@cine.com", new Empleado("Beto", "Beto@CINE.com", "{bcrypt}x", Rol.ACOMODADOR).getEmail());
    }

    @Test
    void nombreYEmailSeGuardanSinEspaciosDeMasYSeMidenYaRecortados() {
        Cliente cliente = new Cliente("  Ana ", " ana@cine.com  ");
        Empleado empleado = new Empleado(" " + "x".repeat(100) + " ", " e@cine.com ", "{bcrypt}x", Rol.ADMINISTRADOR);

        assertEquals("Ana", cliente.getNombre());
        assertEquals("ana@cine.com", cliente.getEmail());
        assertEquals("x".repeat(100), empleado.getNombre());
        assertEquals("e@cine.com", empleado.getEmail());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            ADMINISTRADOR, Encargado, e@cine.com
            ACOMODADOR,    Portero,   p@cine.com
            """)
    void losDosRolesDeEmpleadoSeConstruyen(Rol rol, String nombre, String email) {
        assertEquals(rol, new Empleado(nombre, email, "{bcrypt}x", rol).getRol());
    }
}
