package ar.uade.cine.service.funciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.service.programaciones.GestorProgramaciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.ventas.CalculadoraPrecio;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.service.cartelera.GestorRevisionCartelera;

class GestorFuncionesTest extends PruebaDeIntegracion {

    @Autowired
    private GestorFunciones funciones;
    @Autowired
    private GestorSalas salas;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorRevisionCartelera revision;
    @Autowired
    private GestorReservas reservas;
    @Autowired
    private GestorClientes clientes;

    @BeforeEach
    void prepararCartelera() {
        cartelera.agregar("Interstellar", 120, List.of(Genero.CIENCIA_FICCION), Clasificacion.ATP);
        salas.agregar("Sala 1", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
    }

    private void cargarMasFunciones() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(10, 10));
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));
        funciones.programar(2, 1, LocalDateTime.of(2026, 8, 22, 18, 0),
                Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    @Test
    void buscarSinCriteriosDevuelveTodo() {
        cargarMasFunciones();

        assertEquals(3, funciones.buscar(null, null, null, null).size());
    }

    @Test
    void buscarPorPeliculaYPorSala() {
        cargarMasFunciones();

        assertEquals(2, funciones.buscar(1, null, null, null).size(), "las dos de Interstellar");
        assertEquals(2, funciones.buscar(null, 1, null, null).size(), "las dos de la sala 1");
        assertEquals(1, funciones.buscar(1, 1, null, null).size());
    }

    @Test
    void elRangoDeFechasIncluyeLosDosExtremos() {
        cargarMasFunciones();

        assertEquals(3, funciones.buscar(null, null,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)).size());
        assertEquals(1, funciones.buscar(null, null,
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 22)).size(),
                "un solo día: desde y hasta iguales");
    }

    @Test
    void elRangoSeAbreDeUnLadoODelOtro() {
        cargarMasFunciones();

        assertEquals(1, funciones.buscar(null, null, LocalDate.of(2026, 8, 21), null).size(),
                "solo desde: de ahí en adelante");
        assertEquals(2, funciones.buscar(null, null, null, LocalDate.of(2026, 8, 21)).size(),
                "solo hasta: todo lo anterior");
    }

    @Test
    void buscarSinCoincidenciasDevuelveVacioYNoFalla() {
        cargarMasFunciones();

        assertTrue(funciones.buscar(null, null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)).isEmpty());
        assertTrue(funciones.buscar(99, null, null, null).isEmpty(),
                "una película que no existe no es un error, es cero resultados");
    }

    @Test
    void rechazaFuncionQueEmpiezaMientrasCorreOtra() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 21, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void rechazaFuncionPegadaAlFinalDeLaAnteriorPorLaLimpieza() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void elMensajeExplicaQueElChoqueEsPorLaLimpieza() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 5),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertTrue(e.getMessage().contains("limpieza"), e.getMessage());
        assertTrue(e.getMessage().contains("22:15"), e.getMessage());
    }

    @Test
    void aceptaFuncionCuandoYaTerminoLaLimpieza() {
        assertDoesNotThrow(
                () -> funciones.programar(1, 1, LocalDateTime.of(2026, 8, 20, 22, 15),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
        assertEquals(2, funciones.listar().size());
    }

    @Test
    void sinLimpiezaLasFuncionesSePuedenEncadenar() {
        salas.agregar("Sala sin corte", TipoSala.DOS_D, List.of(10, 10), Map.of(), 0);
        funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500));

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void rechazaSalaConLimpiezaNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> salas.agregar("Sala rota", TipoSala.DOS_D, List.of(10, 10), Map.of(), -5));
    }

    @Test
    void laLimpiezaNoAfectaALasOtrasSalas() {
        salas.agregar("Sala 2", TipoSala.DOS_D, List.of(6, 8), Map.of(), 30);

        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 22, 0),
                        Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void elMismoHorarioEnOtraSalaNoSePisa() {
        salas.agregar("Sala 2", TipoSala.TRES_D, List.of(6, 8));
        assertDoesNotThrow(
                () -> funciones.programar(1, 2, LocalDateTime.of(2026, 8, 20, 20, 0),
                        Version.DOBLADA, Proyeccion.TRES_D, Dinero.de(4500)));
    }

    @Test
    void noSePuedeProgramarUnaPeliculaPendienteDeRevision() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));

        assertTrue(e.getMessage().contains("confirmada"), e.getMessage());
    }

    @Test
    void unaVezConfirmadaSeProgramaNormal() {
        Pelicula importada = revision.importar(
                DatosPelicula.deAlta("Dune", 155, List.of(Genero.CIENCIA_FICCION), Clasificacion.MAS_13));
        revision.confirmar(importada.getId());

        assertDoesNotThrow(
                () -> funciones.programar(importada.getId(), 1, LocalDateTime.of(2026, 8, 25, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void rechazaPeliculaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> funciones.programar(99, 1, LocalDateTime.of(2026, 8, 21, 20, 0),
                        Version.DOBLADA, Proyeccion.DOS_D, Dinero.de(4500)));
    }

    @Test
    void noSeBorraUnaFuncionConReservas() {
        clientes.registrar("Andrei", "andrei@uade.edu.ar");
        reservas.reservar(1, 1, generales("A1"));

        assertThrows(IllegalArgumentException.class, () -> funciones.eliminar(1));
        assertEquals(1, funciones.listar().size());
    }

    @Test
    void unaFuncionSinReservasSeBorra() {
        funciones.eliminar(1);
        assertEquals(0, funciones.listar().size());
    }

    @Test
    void noSeBorraLaSalaNiLaPeliculaConFuncionesProgramadas() {
        assertThrows(IllegalArgumentException.class, () -> salas.eliminar(1));
        assertThrows(IllegalArgumentException.class, () -> cartelera.eliminar(1));

        funciones.eliminar(1);
        assertDoesNotThrow(() -> cartelera.eliminar(1));
        assertDoesNotThrow(() -> salas.eliminar(1));
    }


    private static final int DURACION = 136;

    private Funcion funcionDeLas20() {
        return new Funcion(null, null, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    @Test
    void unaFuncionNoEmpezoAntesDeSuHorario() {
        Funcion funcion = funcionDeLas20();

        assertFalse(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 19, 59)));
        assertTrue(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 20, 0)), "el minuto exacto ya cuenta");
        assertTrue(funcion.yaEmpezo(LocalDateTime.of(2026, 8, 20, 20, 1)));
    }

    @Test
    void elFinSaleDeLaDuracionDeLaPelicula() {
        assertEquals(LocalDateTime.of(2026, 8, 20, 22, 16), funcionDeLas20().getFin(DURACION));
    }

    private static Map<String, TipoTarifa> generales(String... codigos) {
        Map<String, TipoTarifa> butacas = new LinkedHashMap<>();
        for (String codigo : codigos) {
            butacas.put(codigo, TipoTarifa.GENERAL);
        }
        return butacas;
    }
}
