package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.uade.cine.PruebaDeApi;

class PromocionControllerTest extends PruebaDeApi {

    @Test
    void unaFechaMalEscritaEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-13-45\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("el inicio de la vigencia tiene que ser una fecha válida", respuesta.error());
    }

    @Test
    void sinVigenciaEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\",\"porcentaje\":20}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el inicio de la vigencia", respuesta.error());
    }

    @Test
    void unDiaQueNoExisteEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\","
                + "\"diasSemana\":[\"JUEVESITO\"]}");

        assertEquals(400, respuesta.estado());
        assertEquals("Valor inválido para el día: JUEVESITO", respuesta.error());
    }
}
