package ar.uade.cine.controller.promociones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;

import ar.uade.cine.PruebaDeApi;

class PromocionControllerTest extends PruebaDeApi {

    @Test
    void unaFechaMalEscritaEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-13-45\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El inicio de la vigencia no es válido: usá AAAA-MM-DD", respuesta.error());
    }

    // Con la etiqueta de cada tipo y no con el nombre de la constante.
    @Test
    void unTipoQueNoExisteEs400YNombraLosQueHay() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"REGALO\","
                + "\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El tipo de promoción tiene que ser porcentaje, monto fijo o NxM", respuesta.error());
    }

    @Test
    void sinVigenciaEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\",\"porcentaje\":20}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el inicio de la vigencia", respuesta.error());
    }

    @Test
    void elAltaDevuelveLaUbicacionYElPatchLaDaDeBajaYLaReactiva() {
        Respuesta alta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\"}");
        String ubicacion = alta.cabeceras().getLocation().toString();
        assertEquals("/api/promociones/" + alta.json().get("id").asInt(), ubicacion);

        assertFalse(patch(ubicacion, "{\"activa\":false}").json().get("activa").asBoolean());
        assertFalse(get(ubicacion).json().get("activa").asBoolean());
        assertTrue(patch(ubicacion, "{\"activa\":true}").json().get("activa").asBoolean());
    }

    @Test
    void unPatchSinActivaEs400YSobreUnaQueNoExisteEs404() {
        Respuesta sinCampo = patch("/api/promociones/1", "{}");
        assertEquals(400, sinCampo.estado());
        assertEquals("Falta decir si queda activa", sinCampo.error());

        assertEquals(404, patch("/api/promociones/99", "{\"activa\":false}").estado());
    }

    @Test
    void unDiaQueNoExisteEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\","
                + "\"diasSemana\":[\"JUEVESITO\"]}");

        assertEquals(400, respuesta.estado());
        assertEquals("El día de la semana no es válido: JUEVESITO", respuesta.error());
    }

    @Test
    void losMensajesDelGestorYDeLaEntidadLleganIntactos() {
        String vigencia = ",\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\"}";

        Respuesta sinPaga = post("/api/promociones", "{\"nombre\":\"2x1\",\"tipo\":\"NXM\",\"lleva\":2" + vigencia);
        assertEquals(400, sinPaga.estado());
        assertEquals("Falta cuántas entradas paga", sinPaga.error());

        // El mismo texto que la entidad, que también rechaza un monto que no llegó.
        Respuesta sinMonto = post("/api/promociones", "{\"nombre\":\"Banco\",\"tipo\":\"MONTO_FIJO\"" + vigencia);
        assertEquals("Falta el monto del descuento", sinMonto.error());

        Respuesta dosPorDos = post("/api/promociones",
                "{\"nombre\":\"2x2\",\"tipo\":\"NXM\",\"lleva\":2,\"paga\":2" + vigencia);
        assertEquals(400, dosPorDos.estado());
        assertEquals("En un NxM hay que llevar más de lo que se paga", dosPorDos.error());
    }

    // Lo que valida el pedido (nombre y tipo) y lo que valida el modelo al crear la subclase llegan igual.
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            sin nombre            | "tipo":"PORCENTAJE","porcentaje":20                        | Falta el nombre
            sin tipo              | "nombre":"Martes","porcentaje":20                          | Falta el tipo de promoción
            tipo en blanco        | "nombre":"Martes","tipo":" ","porcentaje":20                | Falta el tipo de promoción
            sin porcentaje        | "nombre":"Martes","tipo":"PORCENTAJE","monto":500          | Falta el porcentaje
            casi cien por ciento  | "nombre":"Martes","tipo":"PORCENTAJE","porcentaje":99.999  | El porcentaje tiene que estar entre 1 y 99
            monto con 3 decimales | "nombre":"Banco","tipo":"MONTO_FIJO","monto":10.555        | El monto del descuento tiene que tener como máximo 2 decimales
            uno por cero          | "nombre":"1x0","tipo":"NXM","lleva":1,"paga":0             | Un NxM tiene que cobrar al menos una entrada
            once por diez         | "nombre":"11x10","tipo":"NXM","lleva":11,"paga":10         | Un NxM tiene que llevar como máximo 10 entradas, el tope de butacas por compra
            """)
    void unaPromocionInvalidaEs400ConSuMensaje(String caso, String campos, String mensaje) {
        Respuesta respuesta = post("/api/promociones",
                "{" + campos + ",\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.error());
    }

    // Una hora vacía es una hora que no vino, como en el resto de la API: antes decía «Falta la hora de inicio».
    @Test
    void lasHorasVaciasDejanLaFranjaAbierta() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Martes\",\"tipo\":\"PORCENTAJE\","
                + "\"porcentaje\":20,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\","
                + "\"horaDesde\":\"\",\"horaHasta\":\" \"}");

        assertEquals(201, respuesta.estado());
        assertNull(respuesta.json().get("horaDesde"));
        assertNull(respuesta.json().get("horaHasta"));
    }

    @Test
    void unaPromocionQueNoExisteEs404() {
        Respuesta respuesta = get("/api/promociones/99");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la promoción 99", respuesta.error());
    }

    // DECIMAL(10,2) no guarda cien millones: sin el tope, MySQL rechazaba el INSERT con un 500.
    @Test
    void unMontoDeCienMillonesEs400() {
        Respuesta respuesta = post("/api/promociones", "{\"nombre\":\"Banco\",\"tipo\":\"MONTO_FIJO\","
                + "\"monto\":100000000,\"vigenciaDesde\":\"2026-09-01\",\"vigenciaHasta\":\"2026-12-31\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("El monto del descuento no puede superar $ 1000000.00", respuesta.error());
    }

    // Sin credenciales: la ve cualquiera. La vencida y la dada de baja no salen.
    @Test
    void lasVigentesSonPublicasYSoloLasActivasSinTerminar() {
        post("/api/promociones", "{\"nombre\":\"Miércoles 2x1\",\"tipo\":\"NXM\",\"lleva\":2,\"paga\":1,"
                + "\"vigenciaDesde\":\"2026-08-01\",\"vigenciaHasta\":\"2026-12-31\",\"diasSemana\":[\"WEDNESDAY\"],"
                + "\"mediosPago\":[\"DEBITO\"]}");
        post("/api/promociones", "{\"nombre\":\"Agosto\",\"tipo\":\"PORCENTAJE\",\"porcentaje\":20,"
                + "\"vigenciaDesde\":\"2026-08-01\",\"vigenciaHasta\":\"2026-08-20\"}");
        int baja = post("/api/promociones", "{\"nombre\":\"Baja\",\"tipo\":\"MONTO_FIJO\",\"monto\":500,"
                + "\"vigenciaDesde\":\"2026-08-01\",\"vigenciaHasta\":\"2026-12-31\"}").json().get("id").asInt();
        patch("/api/promociones/" + baja, "{\"activa\":false}");
        reloj.mover(LocalDateTime.of(2026, 8, 21, 10, 0));

        Respuesta respuesta = pedirComo(HttpMethod.GET, "/api/promociones/vigentes", null, null, null);

        assertEquals(200, respuesta.estado());
        JsonNode promos = respuesta.json();
        assertEquals(1, promos.size());
        JsonNode dosPorUno = promos.get(0);
        assertEquals("Miércoles 2x1", dosPorUno.get("nombre").asText());
        assertEquals("2x1", dosPorUno.get("beneficio").asText());
        assertEquals("WEDNESDAY", dosPorUno.get("diasSemana").get(0).asText());
        assertEquals("DEBITO", dosPorUno.get("mediosPago").get(0).asText());
        assertEquals("[\"MENOR\",\"JUBILADO\",\"ESTUDIANTE\"]", dosPorUno.get("tarifasExcluidas").toString());
        assertNull(dosPorUno.get("id"));
        assertNull(dosPorUno.get("activa"));
    }
}
