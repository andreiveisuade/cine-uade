package ar.uade.cine.swing.pantallas.produccion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Lo del sistema manda sobre lo guardado, olvidar solo borra el token y el token no sale impreso.
class CuentaAivenTest {

    private Preferences preferencias;

    @BeforeEach
    void crear() {
        preferencias = Preferences.userRoot().node("/ar/uade/cine/swing/test-" + UUID.randomUUID());
    }

    @AfterEach
    void borrar() throws BackingStoreException {
        preferencias.removeNode();
    }

    @Test
    void elSistemaMandaSobreLoGuardado() {
        CuentaAiven.guardar(preferencias, "guardado", "mysql", "token-guardado");
        Map<String, String> sistema = Map.of("AIVEN_TOKEN", "token-del-entorno", "AIVEN_PROYECTO", "del-entorno");

        CuentaAiven cuenta = CuentaAiven.leer(preferencias, sistema::get);

        assertEquals("token-del-entorno", cuenta.token());
        assertEquals("del-entorno", cuenta.proyecto());
        assertEquals("mysql", cuenta.servicio());
        assertTrue(cuenta.tokenDelSistema());
    }

    @Test
    void olvidarBorraSoloElTokenYUnoVacioNoPisaElGuardado() {
        Map<String, String> sistema = Map.of();
        CuentaAiven.guardar(preferencias, "p", "s", "token");
        CuentaAiven.guardar(preferencias, "p", "s", "  ");
        assertEquals("token", CuentaAiven.leer(preferencias, sistema::get).token());

        CuentaAiven.olvidarToken(preferencias);

        CuentaAiven cuenta = CuentaAiven.leer(preferencias, sistema::get);
        assertFalse(cuenta.conToken());
        assertEquals("p", cuenta.proyecto());
    }

    @Test
    void elTokenNoSaleEnElTexto() {
        assertFalse(new CuentaAiven("secreto", "p", "s", false).toString().contains("secreto"));
    }
}
