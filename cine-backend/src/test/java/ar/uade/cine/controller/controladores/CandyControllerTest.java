package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.candy.GestorProductos;

/** R14 después de un PUT: el combo sigue saliendo menos que sus componentes, se edite cuál se edite. */
class CandyControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos carta;

    private int pochoclos;
    private int combo;

    /** Pochoclos $4000 + gaseosa $2500 = $6500 sueltos; el combo sale $5500. */
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
    }

    @Test
    void unPrecioEnCeroEs400() {
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos, "{\"nombre\":\"Pochoclos\",\"precio\":0}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio debe ser mayor a cero", respuesta.error());
    }

    @Test
    void elComboNoPuedeQuedarMasCaroQueSusComponentes() {
        Respuesta respuesta = put("/api/candy/productos/" + combo, "{\"nombre\":\"Combo clásico\",\"precio\":7000}");

        assertEquals(400, respuesta.estado());
        assertEquals(5500, get("/api/candy/productos/" + combo).json().get("precio").asDouble());
    }

    @Test
    void abaratarUnComponenteNoPuedeDejarAlComboSinConvenir() {
        // Pochoclos a $2000: sueltos saldrían $4500, menos que los $5500 del combo.
        Respuesta respuesta = put("/api/candy/productos/" + pochoclos,
                "{\"nombre\":\"Pochoclos grandes\",\"precio\":2000}");

        assertEquals(400, respuesta.estado());
        assertEquals("Con ese precio, el combo Combo clásico dejaría de salir menos que sus"
                + " componentes sueltos ($ 4500.00)", respuesta.error());
        assertEquals(4000, get("/api/candy/productos/" + pochoclos).json().get("precio").asDouble());
    }
}
