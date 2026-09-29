package ar.uade.cine.controller.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.service.cartelera.DatosPelicula;
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

        cartelera.agregar(new DatosPelicula("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13,
                null, null, null, null, null, null, 8.7, null));
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

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin precio avisa que falta y no que es inválido,      '{}',                        Falta el precio de las funciones
            # La propuesta se arma en memoria pase por pase: un pedido de años no puede llegar al planificador.
            una grilla de más de un mes es 400,                   '{"precio":5000,"dias":32}', La grilla no puede cubrir más de 31 días
            con precio en cero el mensaje es el del planificador, '{"precio":0}',              El precio debe ser mayor a cero
            # Primero se lee todo el pedido y después se validan los criterios: gana el error de formato.
            con dos errores gana el de formato,                   '{"precio":0,"apertura":"25:00"}', la hora de apertura tiene que ser una hora válida
            """)
    void unaPropuestaInvalidaEs400ConSuMensaje(String caso, String cuerpo, String mensaje) {
        Respuesta respuesta = post("/api/grilla/propuesta", cuerpo);

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.json().get("error").asText());
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
