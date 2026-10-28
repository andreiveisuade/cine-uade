package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiSesion;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.Servidor;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// El servidor del ingreso: qué se preselecciona, qué se recuerda y qué URL no deja ni intentar.
class PanelIngresoTest {

    private static final String EMPLEADO =
            "{\"id\":1,\"nombre\":\"Encargado\",\"email\":\"encargado@cine.uade.ar\",\"rol\":\"ADMINISTRADOR\",\"empleado\":true}";

    private ServidorFalso servidor;
    private Preferences preferencias;
    private String propiedadAnterior;
    private final AtomicReference<Servidor> ingresoEn = new AtomicReference<>();
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        // Un nodo propio por test: no pisa lo que eligió quien corre los tests en su compu.
        preferencias = Preferences.userRoot().node("/ar/uade/cine/swing/test-" + UUID.randomUUID());
        propiedadAnterior = System.getProperty("cine.api.url");
        System.clearProperty("cine.api.url");
    }

    @AfterEach
    void bajar() throws BackingStoreException {
        servidor.bajar();
        if (encargado != null) encargado.cerrar();
        preferencias.removeNode();
        if (propiedadAnterior == null) System.clearProperty("cine.api.url");
        else System.setProperty("cine.api.url", propiedadAnterior);
    }

    private Operador abrir() {
        return Operador.en(() -> new PanelIngreso(preferencias, s -> new ApiSesion(new ClienteHttp(s.url())), null,
                (s, empleado) -> ingresoEn.set(s)));
    }

    private void ingresarEnOtro(Operador operador, String url) {
        operador.elegir("Servidor", "Otro…").tipear("URL", url)
                .tipear("Email", "encargado@cine.uade.ar").tipear("Contraseña", "cine2026").clic("Ingresar");
    }

    @Test
    void sinEleccionPreviaArrancaEnLocal() {
        encargado = abrir();

        assertEquals("Local — localhost:8080", encargado.valor("Servidor"));
    }

    @Test
    void otroConUrlInvalidaNoDejaIngresar() {
        encargado = abrir();

        ingresarEnOtro(encargado, "localhost:8080");

        encargado.esperarTexto("«URL»: «localhost:8080» no es una dirección web");
        assertEquals(0, servidor.veces("POST /api/sesion"));
        assertNull(ingresoEn.get());
    }

    @Test
    void laEleccionSeRecuerdaParaElProximoIngreso() {
        servidor.responder("POST /api/sesion", 200, EMPLEADO);
        encargado = abrir();
        String url = servidor.url().replaceAll("/$", "");

        ingresarEnOtro(encargado, url);
        encargado.esperar(() -> ingresoEn.get() != null);
        encargado.cerrar();

        encargado = abrir();
        assertEquals("Otro…", encargado.valor("Servidor"));
        assertEquals(url, encargado.valor("URL"));
    }

    @Test
    void laPropiedadDelSistemaMandaSobreLoRecordado() {
        preferencias.put(PanelIngreso.CLAVE_SERVIDOR, "http://localhost:8080");
        System.setProperty("cine.api.url", "https://cine-uade.onrender.com");

        encargado = abrir();

        assertEquals("Producción — cine-uade.onrender.com", encargado.valor("Servidor"));
    }
}
