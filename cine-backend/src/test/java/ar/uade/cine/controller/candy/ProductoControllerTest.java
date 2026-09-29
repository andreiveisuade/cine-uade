package ar.uade.cine.controller.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.candy.GestorProductos;

class ProductoControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos carta;

    private int pochoclos;
    private int combo;

    @BeforeEach
    void unaCartaConCombo() {
        pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)).getId();
        int gaseosa = carta.agregar("Gaseosa 500ml", TipoProducto.BEBIDA, Dinero.de(2500)).getId();
        combo = carta.armarCombo("Combo clásico", Dinero.de(5500), Map.of(pochoclos, 1, gaseosa, 1)).getId();
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
    void elComboNoPuedeQuedarMasCaroQueSusComponentes() {
        Respuesta respuesta = put("/api/candy/productos/" + combo, "{\"nombre\":\"Combo clásico\",\"precio\":7000}");

        assertEquals(400, respuesta.estado());
        assertEquals(5500, get("/api/candy/productos/" + combo).json().get("precio").asDouble());
    }

    @Test
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
}
