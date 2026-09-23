package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.salas.TipoSala;

class GrillaControllerTest extends PruebaDeApi {

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorSalas salas;



    @BeforeEach
    void levantarLaApiConUnCine() {

        Pelicula matrix = cartelera
                .agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        matrix.setPuntaje(8.7);
        cartelera.actualizar(matrix);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
    }


    @Test
    void conSoloElPrecioArmaLaGrillaConLosDefaults() {
        Respuesta respuesta = post("/api/grilla/propuesta", "{\"precio\":5000}");

        assertEquals(200, respuesta.estado());
        assertTrue(respuesta.json().get("pases").size() > 0);
        assertEquals(0, respuesta.json().get("funcionesCreadas").asInt(),
                "previsualizar no escribe: el contador queda en cero");
    }

    @Test
    void sinPrecioAvisaQueFaltaYNoQueEsInvalido() {
        Respuesta respuesta = post("/api/grilla/propuesta", "{}");

        assertEquals(400, respuesta.estado());
        assertEquals("Falta el precio de las funciones", respuesta.json().get("error").asText());
    }

    @Test
    void conPrecioEnCeroElMensajeEsElDelPlanificador() {
        Respuesta respuesta = post("/api/grilla/propuesta", "{\"precio\":0}");

        assertEquals(400, respuesta.estado());
        assertEquals("El precio debe ser mayor a cero", respuesta.json().get("error").asText());
    }

    @Test
    void elAltaDevuelve201YCuentaLasFuncionesCreadas() {
        Respuesta respuesta = post("/api/grilla",
                "{\"precio\":5000,\"dias\":1,\"cuantasPeliculas\":1}");

        assertEquals(201, respuesta.estado());
        int creadas = respuesta.json().get("funcionesCreadas").asInt();
        assertTrue(creadas > 0, "el alta tiene que crear funciones");
        assertEquals(creadas, funciones.listar().size());
    }
}
