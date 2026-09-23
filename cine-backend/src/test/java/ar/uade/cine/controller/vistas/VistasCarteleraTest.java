package ar.uade.cine.controller.vistas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.service.ventas.GestorReservas;
import ar.uade.cine.service.usuarios.GestorClientes;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.salas.GestorSalas;
import ar.uade.cine.service.cartelera.GestorCartelera;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorBorderoTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorReciboTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.infrastructure.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.infrastructure.pasarelas.emulada.MercadoPagoEmulado;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.funciones.Proyeccion;
import ar.uade.cine.model.funciones.Version;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.salas.TipoAsiento;
import ar.uade.cine.model.salas.TipoSala;
import ar.uade.cine.model.ventas.TipoTarifa;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.infrastructure.bloqueos.BloqueoButacasMemoria;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.service.cartelera.DatosPelicula;
import ar.uade.cine.service.RecursoNoEncontrado;
import ar.uade.cine.service.ventas.Ocupacion;
import ar.uade.cine.model.dinero.Dinero;

class VistasCarteleraTest extends PruebaDeIntegracion {

    @Autowired
    private GestorCartelera cartelera;

    @Autowired
    private GestorClientes clientes;

    @Autowired
    private GestorFunciones funciones;

    @Autowired
    private GestorReservas reservas;

    @Autowired
    private GestorSalas salas;

    @Autowired
    private VistasCartelera vistas;

    @Test
    void laPeliculaViajaConSusEnumsComoNombre() {
        Pelicula matrix = cartelera
                .agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION),
                        Clasificacion.MAS_13);

        PeliculaVistaDTO vista = vistas.pelicula(matrix);

        assertEquals("Matrix", vista.titulo());
        assertEquals(136, vista.duracionMinutos());
        assertEquals(List.of("ACCION", "CIENCIA_FICCION"), vista.generos());
        assertEquals("MAS_13", vista.clasificacion());
    }

    @Test
    void laPeliculaViajaConSusDatosDeCatalogo() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Pelicula completa = cartelera.editar(1,
                new DatosPelicula(null, null, null, null, "Wachowski",
                        "Un hacker descubre la verdad", 1999, "Inglés", "http://poster.jpg",
                        null, null, null));

        PeliculaVistaDTO vista = vistas.pelicula(completa);

        assertEquals("Wachowski", vista.director());
        assertEquals(1999, vista.anio());
        assertEquals("Inglés", vista.idiomaOriginal());
        assertEquals("http://poster.jpg", vista.posterUrl());
        assertEquals("Un hacker descubre la verdad", vista.sinopsis());
    }

    @Test
    void laFuncionParaElClienteNoTraeNiPeliculaNiButacas() {
        Funcion funcion = programarUnaFuncion();

        FuncionVistaDTO vista = vistas.funcion(funcion);

        assertEquals("Sala 1", vista.sala().nombre());
        assertNull(vista.pelicula());
        assertNull(vista.asientos());
        assertNull(vista.libres());
    }

    @Test
    void laFuncionParaElEncargadoDiceQuePeliculaVa() {
        Funcion funcion = programarUnaFuncion();

        FuncionVistaDTO vista = vistas.funcionConPelicula(funcion);

        assertEquals("Matrix", vista.pelicula().titulo());
        assertNull(vista.asientos(), "el listado no dibuja el mapa de butacas");
    }

    @Test
    void elMapaDeButacasTraeLaSalaEnteraYCuantosLugaresQuedan() {
        Funcion funcion = programarUnaFuncion();

        FuncionVistaDTO vista = vistas.funcionConButacas(funcion);

        assertEquals(10, vista.asientos().size());
        assertEquals(10, vista.libres());
        assertTrue(vista.asientos().stream().noneMatch(a -> a.ocupado()));
    }

    @Test
    void elMapaMarcaLasButacasYaReservadas() {
        Funcion funcion = programarUnaFuncion();
        reservas.reservar(funcion.getId(),
                clientes.identificar("Andrei", "andrei@uade.edu.ar").getId(),
                Map.of("A1", TipoTarifa.GENERAL));

        FuncionVistaDTO vista = vistas.funcionConButacas(funcion);

        assertTrue(butaca(vista, "A1").ocupado());
        assertFalse(butaca(vista, "A2").ocupado());
        assertEquals(9, vista.libres());
    }

    @Test
    void elPrecioDesdeContemplaLaSalaPeroNoElTipoDeButaca() {
        Sala imax = salas.agregar("IMAX", TipoSala.IMAX, List.of(2),
                Map.of("A1", TipoAsiento.VIP));
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Funcion funcion = funciones.programar(1, imax.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        FuncionVistaDTO vista = vistas.funcion(funcion);

        assertEquals(5000.0, vista.precio(), 0.001, "el precio crudo de la función");
        assertEquals(8000.0, vista.precioDesde(), 0.001, "5000 x 1.6 de IMAX, butaca estándar");
    }

    @Test
    void elInicioViajaConElFormatoDelContrato() {
        assertEquals("2026-08-20T20:00:00", vistas.funcion(programarUnaFuncion()).inicio());
    }

    @Test
    void laVersionYLaProyeccionViajanComoNombre() {
        FuncionVistaDTO vista = vistas.funcion(programarUnaFuncion());

        assertEquals("SUBTITULADA", vista.idioma());
        assertEquals("DOS_D", vista.proyeccion());
    }

    @Test
    void unaFuncionSinSalaEsUnRecursoQueNoExiste() {
        Pelicula matrix = cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Sala sinGuardar = new Sala("Sala 9", TipoSala.DOS_D, 15);
        Funcion huerfana = new Funcion(matrix, sinGuardar, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        assertThrows(RecursoNoEncontrado.class, () -> vistas.funcion(huerfana));
    }

    @Test
    void unaFuncionSinPeliculaNoRompeElListado() {
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Pelicula sinGuardar = new Pelicula("Fantasma", 100, List.of(Genero.DRAMA), Clasificacion.ATP);
        Funcion sinPelicula = new Funcion(sinGuardar, sala,
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));

        FuncionVistaDTO vista = vistas.funcionConPelicula(sinPelicula);

        assertNull(vista.pelicula());
        assertNotNull(vista.sala());
    }

    private Funcion programarUnaFuncion() {
        cartelera.agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Sala sala = salas.agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        return funciones.programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, Dinero.de(5000));
    }

    private static AsientoVistaDTO butaca(FuncionVistaDTO vista, String codigo) {
        return vista.asientos().stream()
                .filter(a -> a.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("El mapa no tiene la butaca " + codigo));
    }
}
