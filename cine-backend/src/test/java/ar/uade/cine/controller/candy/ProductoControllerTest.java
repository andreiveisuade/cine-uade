package ar.uade.cine.controller.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ar.uade.cine.model.imagenes.ImagenUrl;
import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.candy.GestorProductos;

class ProductoControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos carta;

    private int pochoclos;
    private int gaseosa;
    private int combo;

    @BeforeEach
    void unaCartaConCombo() {
        pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000), ImagenUrl.NINGUNA).getId();
        gaseosa = carta.agregar("Gaseosa 500ml", TipoProducto.BEBIDA, Dinero.de(2500), ImagenUrl.NINGUNA).getId();
        combo = carta.armarCombo("Combo clásico", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1), ImagenUrl.NINGUNA).getId();
    }

    // La página de promos muestra el ahorro tal cual llega: 4000 + 2500 sueltos contra 5500 en combo.
    @Test
    void laCartaPublicaTraeElPrecioSueltoYElAhorroDelCombo() {
        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/candy/productos/" + combo, null, null, null);

        assertEquals(200, respuesta.estado());
        assertEquals(6500.0, respuesta.json().get("precioSuelto").asDouble());
        assertEquals(1000.0, respuesta.json().get("ahorro").asDouble());
    }

    @Test
    void editaNombreYPrecio() {
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos,
                "{\"nombre\":\"Pochoclos XL\",\"precio\":4500}");

        assertEquals(200, respuesta.estado());
        assertEquals("Pochoclos XL", respuesta.json().get("nombre").asText());
        assertEquals(4500, respuesta.json().get("precio").asDouble());
        assertEquals(4500, get("/api/candy/productos/" + pochoclos).json().get("precio").asDouble());
    }

    @Test
    void unProductoQueNoExisteEs404() {
        Respuesta respuesta = put("/api/candy/productos/99", "{\"nombre\":\"X\",\"precio\":100}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe el producto 99", respuesta.error());
        assertEquals(404, patch("/api/candy/productos/99", "{\"disponible\":false}").estado());
        assertEquals("No existe el producto 99", get("/api/candy/productos/99").error());
    }

    // Sin @Positive en el DTO: Dinero.importe rechaza el precio inválido antes de buscar el nombre repetido.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            alta en cero con nombre repetido    | POST | /api/candy/productos    | {"nombre":"Pochoclos grandes","tipo":"POCHOCLOS","precio":0} | El precio tiene que ser mayor a cero
            combo con tres decimales y repetido | POST | /api/candy/combos       | {"nombre":"Combo clásico","precio":10.555,"componentes":{}} | El precio tiene que tener como máximo 2 decimales
            edición negativa de uno que no está | PUT  | /api/candy/productos/99 | {"nombre":"X","precio":-1}                                     | El precio tiene que ser mayor a cero
            """)
    void unPrecioInvalidoSeRechazaAntesDeBuscarNada(String caso, String metodo, String ruta, String cuerpo,
            String mensaje) {
        Respuesta respuesta = pedirComo(HttpMethod.valueOf(metodo), ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    @Test
    void sinNombreDiceQueFalta() {
        Respuesta respuesta = post("/api/candy/productos", "{\"nombre\":\" \",\"tipo\":\"BEBIDA\",\"precio\":100}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el nombre", respuesta.error());
    }

    @Test
    void unPrecioEnCeroEs400() {
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos, "{\"nombre\":\"Pochoclos\",\"precio\":0}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio tiene que ser mayor a cero", respuesta.error());
    }

    // Bean Validation y el producto dicen lo mismo: el texto no depende de por dónde entró el pedido.
    @Test
    void sinPrecioDiceQueFalta() {
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos, "{\"nombre\":\"Pochoclos\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el precio", respuesta.error());
    }

    // DECIMAL(10,2) no guarda cien millones: sin el tope, MySQL rechazaba el INSERT con un 500.
    @Test
    void unPrecioDeCienMillonesEs400() {
        Respuesta respuesta = post("/api/candy/productos",
                "{\"nombre\":\"Agua\",\"tipo\":\"BEBIDA\",\"precio\":100000000}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio no puede superar $ 1000000.00", respuesta.error());
    }

    @Test
    @DisplayName("R14: el combo editado no puede quedar más caro que sus componentes")
    void elComboNoPuedeQuedarMasCaroQueSusComponentes() {
        Respuesta respuesta = put("/api/candy/productos/" + combo, "{\"nombre\":\"Combo clásico\",\"precio\":7000}");

        assertEquals(400, respuesta.estado());
        assertEquals(5500, get("/api/candy/productos/" + combo).json().get("precio").asDouble());
    }

    @Test
    @DisplayName("R14: abaratar un componente no puede dejar al combo sin convenir")
    void abaratarUnComponenteNoPuedeDejarAlComboSinConvenir() {
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos,
                "{\"nombre\":\"Pochoclos grandes\",\"precio\":2000}");

        assertEquals(400, respuesta.estado());
        assertEquals("Con ese precio, el combo Combo clásico dejaría de salir menos que sus"
                + " componentes sueltos ($ 4500.00)", respuesta.error());
        assertEquals(4000, get("/api/candy/productos/" + pochoclos).json().get("precio").asDouble());
    }

    @Test
    void unProductoSacadoDeLaCartaSeRepone() {
        String ruta = "/api/candy/productos/" + pochoclos;
        assertEquals(200, patch(ruta, "{\"disponible\":false}").estado());
        assertEquals(false, enLaCarta(pochoclos));

        Respuesta respuesta = patch(ruta, "{\"disponible\":true}");

        assertEquals(200, respuesta.estado());
        assertEquals(true, respuesta.json().get("disponible").asBoolean());
        assertEquals(true, enLaCarta(pochoclos));
    }

    private boolean enLaCarta(int id) {
        for (var producto : get("/api/candy/productos").json()) {
            if (producto.get("id").asInt() == id) {
                return true;
            }
        }
        return false;
    }

    private static final String IMAGEN = "/api/imagenes/0f8fad5b-d9cb-469f-a165-70867728950e.png";

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            alta    | POST | /api/candy/productos | {"nombre":"Agua","tipo":"BEBIDA","precio":1500,"imagenUrl":"IMAGEN"}
            combo   | POST | /api/candy/combos    | {"nombre":"Combo grande","precio":6000,"componentes":{"POCHOCLOS":1,"GASEOSA":1},"imagenUrl":"IMAGEN"}
            edicion | PUT  | /api/candy/productos/POCHOCLOS | {"nombre":"Pochoclos grandes","precio":4000,"imagenUrl":"IMAGEN"}
            """)
    void laImagenSubidaSeGuardaYSaleEnLaCartaPublica(String caso, String metodo, String ruta, String cuerpo) {
        Respuesta respuesta = pedirComo(HttpMethod.valueOf(metodo), conIds(ruta), conIds(cuerpo).replace("IMAGEN", IMAGEN),
                EMAIL_ADMIN, CLAVE_ADMIN);

        int id = respuesta.json().get("id").asInt();
        assertEquals(IMAGEN, respuesta.json().get("imagenUrl").asText());
        assertEquals(IMAGEN, pedirComo(HttpMethod.GET, "/api/candy/productos/" + id, null, null, null)
                .json().get("imagenUrl").asText());
    }

    private String conIds(String texto) {
        return texto.replace("POCHOCLOS", String.valueOf(pochoclos)).replace("GASEOSA", String.valueOf(gaseosa));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            alta    | POST | /api/candy/productos | {"nombre":"Agua","tipo":"BEBIDA","precio":1500,"imagenUrl":"https://otro.com/agua.png"}
            combo   | POST | /api/candy/combos    | {"nombre":"Combo grande","precio":6000,"componentes":{},"imagenUrl":"/api/imagenes/../x.png"}
            edicion | PUT  | /api/candy/productos/1 | {"nombre":"Pochoclos grandes","precio":4000,"imagenUrl":"/api/candy/productos/1"}
            """)
    void unaImagenQueNoSeSubioAlSistemaEs400(String caso, String metodo, String ruta, String cuerpo) {
        Respuesta respuesta = pedirComo(HttpMethod.valueOf(metodo), ruta, cuerpo, EMAIL_ADMIN, CLAVE_ADMIN);

        assertEquals(400, respuesta.estado());
        assertEquals("La imagen tiene que ser una subida al sistema", respuesta.error());
    }

    @Test
    void sinImagenLaCartaTraeImagenUrlEnNull() {
        assertTrue(get("/api/candy/productos/" + pochoclos).json().get("imagenUrl").isNull());
    }
}
