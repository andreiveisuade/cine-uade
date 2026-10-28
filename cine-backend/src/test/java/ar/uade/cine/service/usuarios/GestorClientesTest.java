package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.usuarios.Rol;
import ar.uade.cine.repository.usuarios.ClienteRepository;

class GestorClientesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorClientes gestor;

    @Autowired
    private GestorEmpleados empleados;

    @Autowired
    private ClienteRepository clienteRepository;

    // Antes eran dos clientes: se comparaba el email exacto.
    @Test
    void elEmailNoDistingueMayusculas() {
        Cliente beto = gestor.ingresarConGoogle("Beto", "beto@x.com");

        assertEquals(beto.getId(), gestor.ingresarConGoogle("Beto", " BETO@x.com ").getId());
        assertEquals(beto.getId(), gestor.buscarPorEmail(" BETO@x.com ").orElseThrow().getId());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void conGoogleUnEmailSinDominioSeRechaza() {
        Rechazo error = assertThrows(Rechazo.class, () -> gestor.ingresarConGoogle("Ana", "a@"));

        assertEquals("El email tiene que tener la forma usuario@dominio.com", error.getMessage());
        assertTrue(gestor.listar().isEmpty());
    }

    @Test
    void buscarPorEmailIgnoraLosEspaciosYSinEmailNoEncuentraANadie() {
        gestor.ingresarConGoogle("Andrei", "andrei@uade.edu.ar");

        assertTrue(gestor.buscarPorEmail("  andrei@uade.edu.ar ").isPresent());
        assertTrue(gestor.buscarPorEmail(null).isEmpty());
        assertTrue(gestor.buscarPorEmail("  ").isEmpty());
    }

    // R28: Google ya probó el email: es el mismo cliente que compró sin cuenta, con el nombre que dio al comprar.
    @Test
    void conGoogleQuienComproSinCuentaEsElMismoCliente() {
        Cliente compro = clienteRepository.save(new Cliente("Andrei", "andrei@uade.edu.ar"));

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
