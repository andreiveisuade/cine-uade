package ar.uade.cine.controller.candy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.candy.GestorProductos;

class CompraCandyControllerTest extends PruebaDeApi {

    @Autowired
    private GestorProductos carta;

    @Test
    void unaVentaConUnaCantidadEnNullEs400YNo500() {
        int pochoclos = carta.agregar("Pochoclos grandes", TipoProducto.POCHOCLOS, Dinero.de(4000)).getId();

        Respuesta respuesta = post("/api/candy/compras",
                "{\"cantidades\":{\"" + pochoclos + "\":null},\"medio\":\"EFECTIVO\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta la cantidad de Pochoclos grandes", respuesta.error());
    }
}
