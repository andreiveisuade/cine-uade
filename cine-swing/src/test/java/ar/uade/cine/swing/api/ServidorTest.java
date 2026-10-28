package ar.uade.cine.swing.api;

import ar.uade.cine.swing.api.Servidor.Entorno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La marca y la espera salen de la URL: la de producción se reconoce aunque venga escrita a mano. */
class ServidorTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "producción            | https://cine-uade.onrender.com   | PRODUCCION | PRODUCCIÓN",
            "producción con barra  | HTTPS://cine-uade.onrender.com/  | PRODUCCION | PRODUCCIÓN",
            "local                 | http://localhost:8080            | LOCAL      | LOCAL",
            "otro con puerto       | http://192.168.0.10:8080/        | OTRO       | OTRO: 192.168.0.10:8080",
            "otro sin puerto       | https://cine.example.com         | OTRO       | OTRO: cine.example.com"})
    void laMarcaDiceElEntornoDeLaUrl(String caso, String url, Entorno entorno, String marca) {
        Servidor servidor = Servidor.de(url);

        assertEquals(entorno, servidor.entorno());
        assertEquals(marca, servidor.marca());
    }

    @Test
    void soloProduccionDuermeYEsperaMas() {
        assertTrue(Servidor.PRODUCCION.duerme());
        assertFalse(Servidor.LOCAL.duerme());
        assertTrue(Servidor.PRODUCCION.esperaDelIngreso().compareTo(Servidor.LOCAL.esperaDelIngreso()) > 0);
    }

    @Test
    void laPropiedadDelSistemaMandaComoServidorConfigurado() {
        String anterior = System.getProperty("cine.api.url");
        try {
            System.setProperty("cine.api.url", "http://otro:9090");
            assertEquals(new Servidor(Entorno.OTRO, "http://otro:9090"), Servidor.configurado().orElseThrow());
        } finally {
            if (anterior == null) System.clearProperty("cine.api.url");
            else System.setProperty("cine.api.url", anterior);
        }
    }
}
