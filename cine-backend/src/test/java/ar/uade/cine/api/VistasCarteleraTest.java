package ar.uade.cine.api;

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
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.Aplicacion;
import ar.uade.cine.comprobantes.txt.GeneradorTicketCandyTxt;
import ar.uade.cine.comprobantes.txt.GeneradorTicketTxt;
import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.dominio.funciones.Funcion;
import ar.uade.cine.dominio.funciones.FuncionImpl;
import ar.uade.cine.dominio.funciones.Proyeccion;
import ar.uade.cine.dominio.funciones.Version;
import ar.uade.cine.dominio.salas.Sala;
import ar.uade.cine.dominio.salas.TipoAsiento;
import ar.uade.cine.dominio.salas.TipoSala;
import ar.uade.cine.dominio.ventas.TipoTarifa;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.persistencia.memoria.AsientoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ClienteDAOMemoria;
import ar.uade.cine.persistencia.memoria.CompraCandyDAOMemoria;
import ar.uade.cine.persistencia.memoria.EmpleadoDAOMemoria;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PagoDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProductoDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PromocionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import ar.uade.cine.servicio.DatosPelicula;

/**
 * Una función arrastra más de lo que guarda: la sala, la película, el precio de cada
 * butaca y cuáles están tomadas. Estas pruebas fijan qué trae cada variante —la del
 * cliente, la del encargado y la del mapa de butacas— porque el front decide qué dibujar
 * según qué campos vengan, y mandar de más es tan un error como mandar de menos.
 */
class VistasCarteleraTest {

    @TempDir
    Path tempDir;

    private Aplicacion aplicacion;
    private VistasCartelera vistas;

    @BeforeEach
    void prepararEscenario() {
        aplicacion = new Aplicacion(
                new PeliculaDAOMemoria(), new SalaDAOMemoria(), new AsientoDAOMemoria(),
                new FuncionDAOMemoria(), new ClienteDAOMemoria(), new EmpleadoDAOMemoria(),
                new ReservaDAOMemoria(), new PagoDAOMemoria(),
                new PromocionDAOMemoria(), new ProgramacionDAOMemoria(), new ProductoDAOMemoria(),
                new CompraCandyDAOMemoria(),
                new GeneradorTicketTxt(tempDir.resolve("tickets")),
                new GeneradorTicketCandyTxt(tempDir.resolve("tickets")));

        vistas = new VistasCartelera(aplicacion.getCartelera(), aplicacion.getSalas(),
                aplicacion.getOcupacion(), aplicacion.getCalculadoraPrecio(),
                new VistasSalas(aplicacion.getSalas(), aplicacion.getCalculadoraPrecio()));
    }

    // ---------- la película ----------

    @Test
    void laPeliculaViajaConSusEnumsComoNombre() {
        Pelicula matrix = aplicacion.getCartelera()
                .agregar("Matrix", 136, List.of(Genero.ACCION, Genero.CIENCIA_FICCION),
                        Clasificacion.MAS_13);

        PeliculaVistaDTO vista = vistas.pelicula(matrix);

        assertEquals("Matrix", vista.titulo());
        assertEquals(136, vista.duracionMinutos());
        assertEquals(List.of("ACCION", "CIENCIA_FICCION"), vista.generos());
        assertEquals("MAS_13", vista.clasificacion());
    }

    /** Los datos de catálogo son los que llenan la ficha del cliente. */
    @Test
    void laPeliculaViajaConSusDatosDeCatalogo() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.MAS_13);
        Pelicula completa = aplicacion.getCartelera().editar(1,
                DatosPelicula.deCatalogo("Wachowski", "Un hacker descubre la verdad", 1999,
                        "Inglés", "http://poster.jpg"));

        PeliculaVistaDTO vista = vistas.pelicula(completa);

        assertEquals("Wachowski", vista.director());
        assertEquals(1999, vista.anio());
        assertEquals("Inglés", vista.idiomaOriginal());
        assertEquals("http://poster.jpg", vista.posterUrl());
        assertEquals("Un hacker descubre la verdad", vista.sinopsis());
    }

    // ---------- las tres variantes de función ----------

    /** El listado del cliente: la función con su sala, sin película ni mapa de butacas. */
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

    /**
     * Qué butaca está tomada se le pregunta a Ocupacion: es una regla de negocio y
     * no una cuestión de formato. Si la vista lo recalculara por su cuenta, el mapa y la
     * validación de la reserva podrían no coincidir.
     */
    @Test
    void elMapaMarcaLasButacasYaReservadas() {
        Funcion funcion = programarUnaFuncion();
        aplicacion.getReservas().reservar(funcion.getId(),
                aplicacion.getClientes().identificar("Andrei", "andrei@uade.edu.ar").getId(),
                Map.of("A1", TipoTarifa.GENERAL));

        FuncionVistaDTO vista = vistas.funcionConButacas(funcion);

        assertTrue(butaca(vista, "A1").ocupado());
        assertFalse(butaca(vista, "A2").ocupado());
        assertEquals(9, vista.libres());
    }

    // ---------- el precio anunciado ----------

    /**
     * El "desde $" de la cartelera: lo que sale la butaca más barata de esa función. No
     * es el precio de la función, que no contempla la tecnología de la sala.
     */
    @Test
    void elPrecioDesdeContemplaLaSalaPeroNoElTipoDeButaca() {
        Sala imax = aplicacion.getSalas().agregar("IMAX", TipoSala.IMAX, List.of(2),
                Map.of("A1", TipoAsiento.VIP));
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Funcion funcion = aplicacion.getFunciones().programar(1, imax.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

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

    /**
     * Una función sin sala es un 404, no un 500: el recurso que se pide no existe. La
     * distinción importa porque el front muestra mensajes distintos.
     */
    @Test
    void unaFuncionSinSalaEsUnRecursoQueNoExiste() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Funcion huerfana = new FuncionImpl(1, 99, LocalDateTime.of(2026, 8, 20, 20, 0),
                Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        assertThrows(NoEncontrado.class, () -> vistas.funcion(huerfana));
    }

    /** El listado del encargado no se cae por una película borrada: la manda en null. */
    @Test
    void unaFuncionSinPeliculaNoRompeElListado() {
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        Funcion sinPelicula = new FuncionImpl(99, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);

        FuncionVistaDTO vista = vistas.funcionConPelicula(sinPelicula);

        assertNull(vista.pelicula());
        assertNotNull(vista.sala());
    }

    private Funcion programarUnaFuncion() {
        aplicacion.getCartelera().agregar("Matrix", 136, List.of(Genero.ACCION), Clasificacion.ATP);
        Sala sala = aplicacion.getSalas().agregar("Sala 1", TipoSala.DOS_D, List.of(5, 5));
        return aplicacion.getFunciones().programar(1, sala.getId(),
                LocalDateTime.of(2026, 8, 20, 20, 0), Version.SUBTITULADA, Proyeccion.DOS_D, 5000);
    }

    private static AsientoVistaDTO butaca(FuncionVistaDTO vista, String codigo) {
        return vista.asientos().stream()
                .filter(a -> a.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("El mapa no tiene la butaca " + codigo));
    }
}
