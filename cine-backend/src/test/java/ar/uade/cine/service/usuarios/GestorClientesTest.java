package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Rol;

class GestorClientesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorClientes gestor;

    @Autowired
    private GestorEmpleados empleados;

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

    @Test
    void identificarIgnoraLosEspaciosDeMasEnElEmail() {
        Cliente primera = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        Cliente conEspacios = gestor.identificar("Andrei", "  andrei@uade.edu.ar  ");

        assertEquals(primera.getId(), conEspacios.getId());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void identificarRechazaUnEmailInvalido() {
        assertThrows(Rechazo.class, () -> gestor.identificar("Andrei", "sin-arroba"));
        assertThrows(Rechazo.class, () -> gestor.identificar("", "nuevo@uade.edu.ar"));
    }

    // Al comprar, el email es un dato más del formulario: un 409 la web lo toma como butaca perdida.
    // El texto no dice que es de un empleado: la compra es pública y delataría al personal.
    @Test
    void identificarConElEmailDeUnEmpleadoEsUnDatoInvalidoYNoUnConflicto() {
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);

        assertEquals("No se puede comprar con ese email: usá otro",
                assertThrowsExactly(DatoInvalido.class,
                        () -> gestor.identificar("Ana", "encargado@cine.com")).getMessage());
    }

    // Antes eran dos clientes: se comparaba el email exacto.
    @Test
    void elEmailNoDistingueMayusculas() {
        Cliente beto = gestor.identificar("Beto", "beto@x.com");

        assertEquals(beto.getId(), gestor.ingresarConGoogle("Beto", "BETO@x.com").getId());
        assertEquals(beto.getId(), gestor.identificar("Beto", " Beto@X.com ").getId());
        assertEquals(beto.getId(), gestor.buscarPorEmail(" BETO@x.com ").orElseThrow().getId());
        assertEquals(1, gestor.listar().size());
    }

    // POST /api/reservas no pasa por el DTO: la forma del email la tiene que exigir Usuario.
    @Test
    void identificarRechazaUnEmailSinDominio() {
        Rechazo error = assertThrows(Rechazo.class,
                () -> gestor.identificar("Ana", "a@"));

        assertEquals("El email tiene que tener la forma usuario@dominio.com", error.getMessage());
    }

    @Test
    void buscarPorEmailIgnoraLosEspaciosYSinEmailNoEncuentraANadie() {
        gestor.identificar("Andrei", "andrei@uade.edu.ar");

        assertTrue(gestor.buscarPorEmail("  andrei@uade.edu.ar ").isPresent());
        assertTrue(gestor.buscarPorEmail(null).isEmpty());
        assertTrue(gestor.buscarPorEmail("  ").isEmpty());
    }

    // R28: Google ya probó el email: es el mismo cliente que compró sin cuenta, con el nombre que dio al comprar.
    @Test
    void conGoogleQuienComproSinCuentaEsElMismoCliente() {
        Cliente compro = gestor.identificar("Andrei", "andrei@uade.edu.ar");

        Cliente conGoogle = gestor.ingresarConGoogle("Andrei Veis", " Andrei@UADE.edu.ar");

        assertEquals(compro.getId(), conGoogle.getId());
        assertEquals("Andrei", conGoogle.getNombre());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void conGoogleUnEmailNuevoEsUnClienteConElNombreDeGoogle() {
        Cliente cliente = gestor.ingresarConGoogle("Andrei Veis", "andrei@uade.edu.ar");

        assertTrue(cliente.getId() > 0);
        assertEquals("Andrei Veis", cliente.getNombre());
        assertEquals(cliente.getId(), gestor.ingresarConGoogle("Andrei Veis", "andrei@uade.edu.ar").getId());
    }

    @Test
    void conGoogleElEmailDeUnEmpleadoSeRechaza() {
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);

        assertEquals("Ese email es del personal del cine: entrá desde la aplicación de escritorio",
                assertThrows(ConflictoDeNegocio.class,
                        () -> gestor.ingresarConGoogle("Encargado", "encargado@cine.com")).getMessage());
        assertTrue(gestor.listar().isEmpty());
    }
}
