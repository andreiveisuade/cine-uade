package ar.uade.cine.swing.pantallas.usuarios;

import ar.uade.cine.swing.api.ApiClientes;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Buscar un cliente por email antes de asignarle clave temporal: qué busca, qué muestra y qué rechaza sin pedir.
class PantallaClientesTest {

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        encargado = Operador.en(() -> new PantallaClientes(new ApiClientes(new ClienteHttp(servidor.url()))));
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void elEncontradoSeMuestraConSuNombre() {
        servidor.responder("GET /api/clientes?email=ana%40mail.com", 200,
                "[{\"id\":5,\"nombre\":\"Ana Prueba\",\"email\":\"ana@mail.com\"}]");
        encargado.tipear("Email", " ana@mail.com ").clic("Buscar");

        encargado.esperarTexto("Ana Prueba");
        assertFalse(encargado.hayBoton("Asignar clave temporal"), "el cliente entra con Google: no tiene clave");
    }

    @Test
    void sinClienteConEseEmailLoDiceJuntoAlCampo() {
        servidor.responder("GET /api/clientes?email=nadie%40mail.com", 200, "[]");

        encargado.tipear("Email", "nadie@mail.com").clic("Buscar");

        encargado.esperarTexto("No hay ningún cliente con el email nadie@mail.com");
        assertFalse(encargado.hayBoton("Asignar clave temporal"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "vacío        | ''          | Falta completar «Email»",
            "sin dominio  | ana@mail    | «Email»: «ana@mail» no es un email"})
    void unEmailMalEscritoNoSeBusca(String caso, String tipeado, String mensaje) {
        encargado.tipear("Email", tipeado).clic("Buscar");

        encargado.esperarTexto(mensaje);
        assertEquals(0, servidor.veces("GET /api/clientes?email=" + tipeado));
    }
}
