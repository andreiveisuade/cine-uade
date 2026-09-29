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
            sin precio avisa que falta y no que es inválido,      '{}',                        Falta el precio
            # La propuesta se arma en memoria pase por pase: un pedido de años no puede llegar al planificador.
            una grilla de más de un mes es 400,                   '{"precio":5000,"dias":32}', La grilla no puede cubrir más de 31 días
            con precio en cero el mensaje es el de las funciones, '{"precio":0}',              El precio tiene que ser mayor a cero
            # DECIMAL(10,2) no guarda cien millones: sin el tope, MySQL rechazaba el INSERT con un 500.
            un precio de cien millones es 400,                    '{"precio":100000000}',      El precio no puede superar $ 1000000.00
            # El controller convierte el precio antes que los otros campos, como cuando lo rechazaba el DTO:
            # el mismo orden que en funciones y programaciones.
            un precio negativo gana contra una hora inválida,     '{"precio":-1,"apertura":"25:00"}', El precio tiene que ser mayor a cero
            # hoy es el 14/08/2026: una grilla desde ayer daba 201 sin crear ninguna función (R20).
            una grilla que empieza en el pasado es 400,           '{"precio":5000,"desde":"2026-08-13"}', La grilla no puede empezar en el pasado
            más de veinte películas es 400,                       '{"precio":5000,"cuantasPeliculas":21}', La grilla no puede tener más de 20 películas
            # Primero se lee todo el pedido y después se validan los criterios: gana el error de formato.
            con dos errores gana el de formato,                   '{"precio":5000,"dias":0,"apertura":"25:00"}', 'La hora de apertura no es válida: usá HH:MM'
            """)
    void unaPropuestaInvalidaEs400ConSuMensaje(String caso, String cuerpo, String mensaje) {
        Respuesta respuesta = post("/api/grilla/propuesta", cuerpo);

        assertEquals(400, respuesta.estado());
        assertEquals(mensaje, respuesta.json().get("error").asText());
    }

    // Como las fechas y las horas: antes un "" decía «Falta el idioma» en un campo que no es obligatorio.
    @Test
    void unIdiomaYUnaProyeccionVaciosCuentanComoNoEnviados() {
        Respuesta respuesta = post("/api/grilla/propuesta", "{\"precio\":5000,\"idioma\":\"\",\"proyeccion\":\" \"}");

        assertEquals(200, respuesta.estado());
        assertTrue(respuesta.json().get("pases").size() > 0);
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
