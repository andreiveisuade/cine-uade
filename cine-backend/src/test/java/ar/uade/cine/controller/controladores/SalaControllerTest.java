package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;

class SalaControllerTest extends PruebaDeApi {

    @Autowired
    private GestorSalas salas;

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorFunciones funciones;

    private int sala;

    @BeforeEach
    void unaSalaDeDosFilas() {
        sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5)).getId();
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(5));
    }

    @Test
    void editaNombreTipoYLimpiezaSinTocarLasButacas() {
        Respuesta respuesta = put("/api/salas/" + sala,
                "{\"nombre\":\"Sala Premium\",\"tipo\":\"IMAX\",\"minutosLimpieza\":25}");

        assertEquals(200, respuesta.estado());
        var json = respuesta.json();
        assertEquals("Sala Premium", json.get("nombre").asText());
        assertEquals("IMAX", json.get("tipo").asText());
        assertEquals(25, json.get("minutosLimpieza").asInt());
        assertEquals(10, json.get("capacidadSala").asInt());
        assertEquals("Sala Premium", get("/api/salas/" + sala).json().get("nombre").asText());
    }

    @Test
    void sinLimpiezaConservaLaQueTenia() {
        Respuesta respuesta = put("/api/salas/" + sala, "{\"nombre\":\"Sala 1\",\"tipo\":\"DOS_D\"}");

        assertEquals(200, respuesta.estado());
        assertEquals(15, respuesta.json().get("minutosLimpieza").asInt());
    }

    @Test
    void unaSalaQueNoExisteEs404() {
        Respuesta respuesta = put("/api/salas/99", "{\"nombre\":\"X\",\"tipo\":\"DOS_D\"}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la sala 99", respuesta.error());
    }

    @Test
    void unNombreQueYaUsaOtraSalaEs400() {
        Respuesta respuesta = put("/api/salas/" + sala, "{\"nombre\":\"sala 2\",\"tipo\":\"DOS_D\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("Ya existe una sala con ese nombre", respuesta.error());
    }

    @Test
    void unTipoQueNoExisteEs400() {
        Respuesta respuesta = put("/api/salas/" + sala, "{\"nombre\":\"Sala 1\",\"tipo\":\"OCHO_D\"}");

        assertEquals(400, respuesta.estado());
    }

    @Test
    void conFuncionesProgramadasNoSeLeCambiaElTipo() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        funciones.programar(1, sala, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        Respuesta respuesta = put("/api/salas/" + sala, "{\"nombre\":\"Sala 1\",\"tipo\":\"IMAX\"}");

        assertEquals(400, respuesta.estado());
        assertEquals("La sala " + sala + " tiene funciones programadas: no se le puede cambiar el tipo",
                respuesta.error());
        assertEquals(200, put("/api/salas/" + sala, "{\"nombre\":\"Sala Uno\",\"tipo\":\"DOS_D\"}").estado());
    }

    @Test
    void unaFilaSinButacasEs400() {
        Respuesta respuesta = post("/api/salas", "{\"nombre\":\"Sala 3\",\"tipo\":\"DOS_D\",\"butacasPorFila\":[5,0]}");

        assertEquals(400, respuesta.estado());
        assertEquals("Cada fila debe tener al menos una butaca", respuesta.error());
    }

    @Test
    void unaButacaFueraDeServicioSeRepone() {
        String ruta = "/api/salas/" + sala + "/asientos/A1";
        assertEquals("FUERA_DE_SERVICIO", estadoDe(put(ruta, "{\"estado\":\"FUERA_DE_SERVICIO\"}"), "A1"));

        assertEquals("HABILITADO", estadoDe(put(ruta, "{\"estado\":\"HABILITADO\"}"), "A1"));
        assertEquals("HABILITADO", estadoDe(get("/api/salas/" + sala), "A1"));
    }

    private static String estadoDe(Respuesta respuesta, String codigo) {
        assertEquals(200, respuesta.estado());
        for (var asiento : respuesta.json().get("asientos")) {
            if (asiento.get("codigo").asText().equals(codigo)) {
                return asiento.get("estado").asText();
            }
        }
        throw new AssertionError("La sala no tiene la butaca " + codigo);
    }
}
