package ar.uade.cine.service.ventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.funciones.FormatoDeFuncion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.rechazos.Rechazo;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;

// R19 sola, sin butacas de por medio: la función de las 20:00 se vende hasta que arranca.
class FuncionesEnVentaTest extends PruebaDeIntegracion {

    @Autowired
    private FuncionesEnVenta enVenta;

    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorFunciones funciones;

    @BeforeEach
    void prepararEscenario() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5));
        funciones.programar(1, 1, LocalDateTime.of(2026, 12, 20, 20, 0),
                new FormatoDeFuncion(Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000)));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "un minuto antes,    2026-12-20T19:59",
            "una semana antes,   2026-12-13T20:00",
    })
    void antesDeEmpezarLaFuncionSeVende(String caso, LocalDateTime ahora) {
        reloj.mover(ahora);

        assertEquals(1, enVenta.exigir(1).getId());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "justo a la hora,      2026-12-20T20:00",
            "empezada hace rato,   2026-12-20T21:30",
    })
    void unaFuncionQueYaEmpezoNoSeVende(String caso, LocalDateTime ahora) {
        reloj.mover(ahora);

        Rechazo error = assertThrows(Rechazo.class, () -> enVenta.exigir(1));

        assertEquals("La función ya empezó: no se pueden reservar butacas", error.getMessage());
    }

    @Test
    void unaFuncionQueNoExisteSeRechazaAntesQueNada() {
        Rechazo error = assertThrows(Rechazo.class, () -> enVenta.exigir(99));

        assertEquals("No existe la función 99", error.getMessage());
    }
}
