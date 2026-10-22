package ar.uade.cine.model.usuarios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void laClaveTemporalAbreLaCuentaYPideCambiarla() {
        Cliente cliente = new Cliente("Ana", "ana@mail.com");

        cliente.asignarClaveTemporal("{bcrypt}hash");

        assertTrue(cliente.tieneCuenta());
        assertTrue(cliente.isDebeCambiarClave());
    }

    @Test
    void cambiarLaClaveDejaDeSerTemporal() {
        Cliente cliente = new Cliente("Ana", "ana@mail.com");
        cliente.asignarClaveTemporal("{bcrypt}hash");

        cliente.cambiarClave("{bcrypt}otro");

        assertFalse(cliente.isDebeCambiarClave());
    }

    // R26: vuelve a ser el cliente que compra sin cuenta, y R22 se la deja reabrir.
    @Test
    void cerrarLaCuentaBorraLaClave() {
        Cliente cliente = new Cliente("Ana", "ana@mail.com");
        cliente.abrirCuenta("{bcrypt}hash");

        cliente.cerrarCuenta();

        assertFalse(cliente.tieneCuenta());
        cliente.abrirCuenta("{bcrypt}nuevo");
        assertTrue(cliente.tieneCuenta());
    }
}
