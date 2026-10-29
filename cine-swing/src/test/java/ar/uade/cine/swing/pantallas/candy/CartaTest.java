package ar.uade.cine.swing.pantallas.candy;

import ar.uade.cine.swing.api.ApiCandy;
import ar.uade.cine.swing.api.ApiCatalogos;
import ar.uade.cine.swing.api.ApiImagenes;
import ar.uade.cine.swing.api.ClienteHttp;
import ar.uade.cine.swing.api.ServidorFalso;
import ar.uade.cine.swing.pantallas.Operador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// La carta del candy: el listado, el alta de un producto suelto y el armado de un combo con sus cantidades.
class CartaTest {

    static final String POCHOCLOS = "{\"id\":1,\"nombre\":\"Pochoclos\",\"tipo\":\"POCHOCLO\",\"precio\":3000.0,"
            + "\"disponible\":true,\"esCombo\":false,\"componentes\":[]}";
    static final String GASEOSA = "{\"id\":2,\"nombre\":\"Gaseosa\",\"tipo\":\"BEBIDA\",\"precio\":2000.0,"
            + "\"disponible\":false,\"esCombo\":false,\"componentes\":[]}";
    static final String COMBO = "{\"id\":3,\"nombre\":\"Combo pareja\",\"tipo\":\"COMBO\",\"precio\":6000.0,"
            + "\"disponible\":true,\"esCombo\":true,\"componentes\":[{\"nombre\":\"Pochoclos\",\"cantidad\":2},"
            + "{\"nombre\":\"Gaseosa\",\"cantidad\":1}]}";

    private ServidorFalso servidor;
    private Operador encargado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = new ServidorFalso();
        servidor.responder("GET /api/tipos-producto", 200, "[{\"nombre\":\"POCHOCLO\",\"esCombo\":false},"
                + "{\"nombre\":\"BEBIDA\",\"esCombo\":false},{\"nombre\":\"COMBO\",\"esCombo\":true}]");
        servidor.responder("GET /api/candy/productos?todos=true", 200,
                "[" + POCHOCLOS + "," + GASEOSA + "," + COMBO + "]");
        ClienteHttp http = new ClienteHttp(servidor.url());
        encargado = Operador.en(() -> new Carta(new ApiCandy(http), new ApiCatalogos(http), new ApiImagenes(http)));
        encargado.esperarFilas(3);
    }

    @AfterEach
    void bajar() {
        servidor.bajar();
        encargado.cerrar();
    }

    @Test
    void listaLaCartaConLoQueTraeCadaCombo() {
        assertEquals("—", encargado.celda(0, "Trae"));
        assertEquals("Fuera de la carta", encargado.celda(1, "Estado"));
        assertEquals("2× Pochoclos + 1× Gaseosa", encargado.celda(2, "Trae"));
        assertEquals("$ 6.000", encargado.celda(2, "Precio"));
    }

    @Test
    void elTipoDeUnProductoSueltoNoOfreceCombo() {
        encargado.esperar(() -> encargado.opciones("Tipo").size() == 2);

        assertFalse(encargado.opciones("Tipo").contains("Combo"));
    }

    @Test
    void agregaUnProductoSuelto() {
        servidor.responder("POST /api/candy/productos", 201, POCHOCLOS);
        encargado.esperar(() -> encargado.opciones("Tipo").size() == 2);
        encargado.tipear("Nombre", "Nachos").elegir("Tipo", encargado.opciones("Tipo").get(1))
                .tipear("Precio", "3500").clic("Agregar a la carta");

        encargado.esperarTexto("Producto agregado");
        assertEquals("{\"nombre\":\"Nachos\",\"tipo\":\"BEBIDA\",\"precio\":3500.0}",
                servidor.ultimo("POST /api/candy/productos").cuerpo());
    }

    @Test
    void sinNombreNiPrecioNoSeAgrega() {
        encargado.clic("Agregar a la carta");

        encargado.esperarTexto("Falta completar «Nombre»");
        encargado.esperarTexto("Falta completar «Precio»");
        assertEquals(0, servidor.veces("POST /api/candy/productos"));
    }

    @Test
    void armaUnComboConLasCantidadesElegidas() {
        servidor.responder("POST /api/candy/combos", 201, COMBO);
        encargado.pestana("Combo").seccion("Armar combo").tipear("Nombre", "Combo pareja")
                .cantidad("Pochoclos  · $ 3.000", 2)
                .cantidad("Gaseosa  · $ 2.000", 1)
                .tipear("Precio del combo", "6000")
                .clic("Armar combo");

        encargado.esperarTexto("Combo armado");
        String cuerpo = servidor.ultimo("POST /api/candy/combos").cuerpo();
        assertTrue(cuerpo.contains("\"componentes\":{\"1\":2,\"2\":1}"), cuerpo);
        assertTrue(cuerpo.startsWith("{\"nombre\":\"Combo pareja\",\"precio\":6000.0"), cuerpo);
    }

    @Test
    void elRechazoDeR14SeMuestraJuntoAlCombo() {
        servidor.responder("POST /api/candy/combos", 400,
                "{\"error\":\"El combo tiene que salir menos que sus componentes sueltos\"}");
        encargado.pestana("Combo").seccion("Armar combo").tipear("Nombre", "Combo caro").tipear("Precio del combo", "9000")
                .clic("Armar combo");

        encargado.esperarTexto("El combo tiene que salir menos que sus componentes sueltos");
    }

    @Test
    void editarSoloSeHabilitaConUnaFilaElegida() {
        assertFalse(encargado.habilitado("Editar"));

        encargado.elegirFila(0);

        assertTrue(encargado.habilitado("Editar"));
    }
}
