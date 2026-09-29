package ar.uade.cine.controller.catalogos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ar.uade.cine.PruebaDeApi;

class CatalogoControllerTest extends PruebaDeApi {

    // Sin credenciales, como los demás catálogos: el cliente también los lee.
    private Respuesta getPublico(String ruta) {
        return pedirComo(HttpMethod.GET, ruta, null, null, null);
    }

    @Test
    void losTiposDeProductoDicenCualEsElCombo() {
        Respuesta respuesta = getPublico("/api/tipos-producto");

        assertEquals(200, respuesta.estado());
        assertEquals("[{\"nombre\":\"POCHOCLOS\",\"esCombo\":false},{\"nombre\":\"BEBIDA\",\"esCombo\":false},"
                + "{\"nombre\":\"GOLOSINA\",\"esCombo\":false},{\"nombre\":\"COMBO\",\"esCombo\":true}]",
                respuesta.cuerpo());
    }

    @Test
    void losTiposDePromocionDicenQueCamposPideCadaUno() {
        Respuesta respuesta = getPublico("/api/tipos-promocion");

        assertEquals(200, respuesta.estado());
        assertEquals("[{\"nombre\":\"PORCENTAJE\",\"campos\":[\"porcentaje\"]},"
                + "{\"nombre\":\"MONTO_FIJO\",\"campos\":[\"monto\"]},"
                + "{\"nombre\":\"NXM\",\"campos\":[\"lleva\",\"paga\"]}]",
                respuesta.cuerpo());
    }

    // El mensaje del gestor sale de la misma lista que publica el catálogo.
    @Test
    void unaPromocionSinElCampoDeSuTipoNombraElQueFalta() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"2x1\",\"tipo\":\"NXM\",\"lleva\":2,"
                + "\"vigenciaDesde\":\"2026-08-01\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta cuántas entradas paga", respuesta.error());
    }
}
