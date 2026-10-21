package ar.uade.cine.service.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final String CLAVE = "clave-del-cliente";

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

    @Test
    void noSeRegistraDosVecesElMismoEmail() {
        gestor.registrar("Andrei", "andrei@uade.edu.ar", CLAVE);

        assertThrows(Rechazo.class,
                () -> gestor.registrar("Otro", "andrei@uade.edu.ar", CLAVE));
    }

    @Test
    void elEmailRepetidoSeComparaSinLosEspaciosDeMas() {
        gestor.registrar("Andrei", "andrei@uade.edu.ar", CLAVE);

        assertThrows(ConflictoDeNegocio.class, () -> gestor.registrar("Otro", "  andrei@uade.edu.ar ", CLAVE));
    }

    // Daba 500: ClienteRepository no ve a los empleados y el INSERT chocaba con el UNIQUE del email.
    @Test
    void elEmailDeUnEmpleadoNoSeRegistraComoCliente() {
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);

        assertEquals("Ya existe un usuario con ese email", assertThrows(ConflictoDeNegocio.class,
                () -> gestor.registrar("Ana", "encargado@cine.com", CLAVE)).getMessage());
    }

    // Al comprar, el email es un dato más del formulario: un 409 la web lo toma como butaca perdida.
    @Test
    void identificarConElEmailDeUnEmpleadoEsUnDatoInvalidoYNoUnConflicto() {
        empleados.registrar("Encargado", "encargado@cine.com", "secreta123", Rol.ADMINISTRADOR);

        assertEquals("Ese email es de un empleado del cine: usá otro para comprar",
                assertThrowsExactly(DatoInvalido.class,
                        () -> gestor.identificar("Ana", "encargado@cine.com")).getMessage());
    }

    @Test
    void elEmailRepetidoEs409ConElTextoDeLaGuia() {
        gestor.registrar("Andrei", "andrei@uade.edu.ar", CLAVE);

        assertEquals("Ya existe una cuenta con ese email", assertThrows(ConflictoDeNegocio.class,
                () -> gestor.registrar("Otro", "andrei@uade.edu.ar", CLAVE)).getMessage());
    }

    // Antes eran dos clientes: se comparaba el email exacto.
    @Test
    void elEmailNoDistingueMayusculas() {
        Cliente beto = gestor.registrar("Beto", "beto@x.com", CLAVE);

        assertThrows(ConflictoDeNegocio.class, () -> gestor.registrar("Beto", "BETO@x.com", CLAVE));
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
        gestor.registrar("Andrei", "andrei@uade.edu.ar", CLAVE);

        assertTrue(gestor.buscarPorEmail("  andrei@uade.edu.ar ").isPresent());
        assertTrue(gestor.buscarPorEmail(null).isEmpty());
        assertTrue(gestor.buscarPorEmail("  ").isEmpty());
    }

    @Test
    void registrarseGuardaLaClaveEnBcryptYNoEnClaro() {
        Cliente cliente = gestor.registrar("Andrei", "andrei@uade.edu.ar", CLAVE);

        assertTrue(cliente.getPasswordHash().startsWith("{bcrypt}"));
        assertTrue(cliente.tieneCuenta());
    }

    // Quien reservó sin cuenta ya es cliente: registrarse le pone clave a ese mismo y conserva sus reservas.
    @Test
    void elQueReservoSinCuentaReclamaSuEmailAlRegistrarse() {
        Cliente sinCuenta = gestor.identificar("Andrei", "andrei@uade.edu.ar");
        assertFalse(sinCuenta.tieneCuenta());

        Cliente conCuenta = gestor.registrar("Andrei", " Andrei@uade.edu.ar ", CLAVE);

        assertEquals(sinCuenta.getId(), conCuenta.getId());
        assertTrue(conCuenta.tieneCuenta());
        assertEquals(1, gestor.listar().size());
    }

    @Test
    void registrarseSinUnaClaveValidaSeRechaza() {
        assertEquals("La contraseña tiene que tener al menos 6 caracteres", assertThrows(DatoInvalido.class,
                () -> gestor.registrar("Andrei", "andrei@uade.edu.ar", "corta")).getMessage());
        assertTrue(gestor.listar().isEmpty());
    }
}
