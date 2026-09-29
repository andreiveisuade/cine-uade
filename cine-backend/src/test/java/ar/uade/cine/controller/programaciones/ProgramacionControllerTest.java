package ar.uade.cine.controller.programaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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
import ar.uade.cine.service.programaciones.DatosGrilla;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.salas.GestorSalas;

class ProgramacionControllerTest extends PruebaDeApi {

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorProgramaciones programaciones;

    @BeforeEach
    void cargarPeliculaYSala() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
    }

    @Test
    void darDeBajaDevuelveLaGrillaYaInactiva() {
        int id = programaciones.crear(new DatosGrilla(1, 1, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13),
                LocalTime.of(20, 30), Set.of(), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)))
                .programacion().getId();

        Respuesta respuesta = patch("/api/programaciones/" + id, "{\"activa\":false}");

        assertEquals(200, respuesta.estado());
        assertFalse(respuesta.json().get("activa").asBoolean());
        assertFalse(get("/api/programaciones/" + id).json().get("activa").asBoolean());
    }

    @Test
    void activarUnaGrillaQueNoExisteEs404ConSuMensaje() {
        Respuesta respuesta = patch("/api/programaciones/99", "{\"activa\":true}");

        assertEquals(404, respuesta.estado());
        assertEquals("No existe la programación 99", respuesta.error());
    }
}
