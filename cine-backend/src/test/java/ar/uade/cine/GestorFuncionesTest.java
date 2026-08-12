package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.uade.cine.interfaces.FuncionDAO;
import ar.uade.cine.interfaces.PeliculaDAO;
import ar.uade.cine.interfaces.SalaDAO;
import ar.uade.cine.modelo.Genero;
import ar.uade.cine.modelo.Idioma;
import ar.uade.cine.modelo.Proyeccion;
import ar.uade.cine.modelo.TipoSala;
import ar.uade.cine.persistencia.FuncionDAOMemoria;
import ar.uade.cine.persistencia.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.AsientoDAOMemoria;
import ar.uade.cine.persistencia.SalaDAOMemoria;
import ar.uade.cine.servicio.GestorCartelera;
import ar.uade.cine.servicio.GestorFunciones;
import ar.uade.cine.servicio.GestorSalas;

/** R3: una sala no puede tener dos funciones superpuestas. */
class GestorFuncionesTest {

    private final PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
    private final SalaDAO salaDAO = new SalaDAOMemoria();
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();

    private GestorFunciones funciones;

    /** Película de 120 minutos en la sala 1. */
    @BeforeEach
    void prepararCartelera() {
        new GestorCartelera(peliculaDAO).agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION));
        new GestorSalas(salaDAO, new AsientoDAOMemoria()).agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones = new GestorFunciones(funcionDAO, peliculaDAO, salaDAO);
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0), Idioma.SUBTITULADA, Proyeccion.DOS_D, 4500);
    }

    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreOtra() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 21, 0), Idioma.SUBTITULADA, Proyeccion.DOS_D, 4500));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void aceptaFuncionDespuesDeQueTerminaLaAnterior() {
        assertDoesNotThrow(
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 0), Idioma.SUBTITULADA, Proyeccion.DOS_D, 4500));
        assertEquals(2, funciones.listar().size());
    }

    @Test
    void elMismoHorarioEnOtraSalaNoSePisa() {
        new GestorSalas(salaDAO, new AsientoDAOMemoria()).agregar("Sala 2", TipoSala.TRES_D, List.of(6, 8));
        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0), Idioma.DOBLADA, Proyeccion.TRES_D, 4500));
    }

    @Test
    void rechazaPeliculaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0), Idioma.DOBLADA, Proyeccion.DOS_D, 4500));
    }
}
