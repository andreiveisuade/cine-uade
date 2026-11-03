package ar.uade.cine.swing.pantallas.salas;

import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiSalas;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Lo de cada tipo de sala sale del catálogo, cruzado por el nombre del tipo: Swing no lo sabe por su cuenta.
class PantallaSalasTest {

    private static final String TIPOS = "[{\"nombre\":\"DOS_D\",\"multiplicador\":1.0,\"soportaTresD\":false,"
            + "\"premium\":false,\"enLaGrilla\":\"2D: las películas en 2D, primero las que todavía no tienen pase "
            + "ese día\"},"
            + "{\"nombre\":\"IMAX\",\"multiplicador\":1.6,\"soportaTresD\":true,\"premium\":true,"
            + "\"enLaGrilla\":\"Premium: las mejor puntuadas, en 3D si tienen versión 3D\"}]";
    private static final String SALAS = "[{\"id\":1,\"nombre\":\"Sala 1\",\"tipo\":\"DOS_D\",\"butacasPorFila\":[8,10],"
            + "\"filas\":2,\"capacidadSala\":18,\"minutosLimpieza\":15},"
            + "{\"id\":3,\"nombre\":\"Sala Grande\",\"tipo\":\"IMAX\",\"butacasPorFila\":[12,14],"
            + "\"filas\":2,\"capacidadSala\":26,\"minutosLimpieza\":20}]";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/salas", 200, SALAS);
        servidor.responder("GET /api/tipos-sala", 200, TIPOS);
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new PantallaSalas(new ApiCatalogos(http), new ApiSalas(http), null));
        encargado.esperarFilas(2);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void cadaSalaMuestraLoQueElCatalogoDiceDeSuTipo() {
        assertEquals("×1,0", encargado.celda(0, "Precio"));
        assertEquals("No", encargado.celda(0, "3D"));
        assertEquals("No", encargado.celda(0, "Premium"));
        assertEquals("×1,6", encargado.celda(1, "Precio"));
        assertEquals("Sí", encargado.celda(1, "3D"));
        assertEquals("Sí", encargado.celda(1, "Premium"));
        encargado.esperarTexto("Elegí una sala para ver qué le toca en la grilla automática");

        encargado.elegirFila(1);

        encargado.esperarTexto("Sala Grande</b> en la grilla automática: "
                + "Premium: las mejor puntuadas, en 3D si tienen versión 3D");
    }
}
