package ar.uade.cine.servicio.cartelera;

import static ar.uade.cine.infraestructura.importador.CatalogoDePrueba.pelicula;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.cartelera.Clasificacion;
import ar.uade.cine.dominio.cartelera.EstadoImportacion;
import ar.uade.cine.dominio.cartelera.EstadoRevision;
import ar.uade.cine.dominio.cartelera.Genero;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dominio.cartelera.ImportacionImpl;
import ar.uade.cine.dominio.cartelera.Pelicula;
import ar.uade.cine.infraestructura.importador.CatalogoDePrueba;
import ar.uade.cine.persistencia.FuncionDAO;
import ar.uade.cine.persistencia.ImportacionDAO;
import ar.uade.cine.persistencia.PeliculaDAO;
import ar.uade.cine.persistencia.memoria.FuncionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ImportacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.PeliculaDAOMemoria;
import ar.uade.cine.persistencia.memoria.ProgramacionDAOMemoria;
import ar.uade.cine.persistencia.memoria.ReservaDAOMemoria;
import ar.uade.cine.persistencia.memoria.SalaDAOMemoria;
import ar.uade.cine.servicio.funciones.GestorFunciones;
import ar.uade.cine.servicio.programaciones.GestorProgramaciones;

/**
 * El gestor contra un catálogo de mentira: acá se prueba qué entra al buzón, qué se saltea y
 * qué queda registrado, no que TMDB conteste.
 *
 * <p>El alta es de verdad —{@link GestorRevisionCartelera} contra los DAO en memoria— porque
 * es justamente lo que se quiso recuperar al absorber el importador: que la corrida pase por
 * las mismas reglas que el alta a mano y que eso se pueda probar sin levantar nada.
 *
 * <p>La espera entre corridas va en cero en el gestor de la mayoría de los tests. Es una
 * protección contra el doble clic y tiene su propio test; dejarla puesta en todos obligaría
 * a que cada uno durmiera un minuto.
 */
class GestorImportacionesTest {

    private final ImportacionDAO importacionDAO = new ImportacionDAOMemoria();
    private final PeliculaDAO peliculaDAO = new PeliculaDAOMemoria();
    private final FuncionDAO funcionDAO = new FuncionDAOMemoria();
    private final CatalogoDePrueba catalogo = new CatalogoDePrueba();

    private final GestorCartelera cartelera = new GestorCartelera(peliculaDAO, funcionDAO,
            new GestorProgramaciones(new ProgramacionDAOMemoria(), funcionDAO,
                    new GestorFunciones(funcionDAO, peliculaDAO, new SalaDAOMemoria(),
                            new ReservaDAOMemoria())));
    private final GestorRevisionCartelera revision =
            new GestorRevisionCartelera(peliculaDAO, funcionDAO, cartelera);

    private final GestorImportaciones gestor = new GestorImportaciones(importacionDAO, catalogo,
            cartelera, revision, Duration.ofMinutes(5), Duration.ZERO);

    @Test
    void unaCorridaQueVuelveBienQuedaRegistradaConLoQueTrajo() {
        catalogo.queTraiga("Duna", "Vaiana");

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.TERMINADA, importacion.getEstado());
        assertEquals(2, importacion.getNuevas());
        assertEquals(0, importacion.getSalteadas());
        assertEquals(0, importacion.getFallidas());
        assertNotNull(importacion.getTerminoEn());
        assertEquals(1, catalogo.consultas());
    }

    /**
     * La propiedad que sostiene todo el circuito: lo que baja de un catálogo ajeno es una
     * propuesta, no una decisión. Si entrara publicada, la cartelera del cine dejaría de ser
     * suya para pasar a ser una copia de lo que se está dando afuera.
     */
    @Test
    void loQueEntraQuedaEnElBuzonYNoEnCartelera() {
        catalogo.queTraiga("Duna");

        gestor.ejecutar(1);

        Pelicula importada = peliculaDAO.listar().get(0);
        assertEquals(EstadoRevision.PENDIENTE, importada.getEstadoRevision());
        assertFalse(importada.estaEnCartelera(), "no puede ofrecerse antes de que la miren");
    }

    /**
     * Lo que hace la corrida repetible: la de mañana no puede fallar entera porque R1 rechaza
     * los títulos que ya están.
     */
    @Test
    void lasQueYaEstanNoSeVuelvenAProponer() {
        catalogo.queTraiga("Duna", "Vaiana");
        gestor.ejecutar(1);

        Importacion segunda = gestor.ejecutar(1);

        assertEquals(0, segunda.getNuevas());
        assertEquals(2, segunda.getSalteadas());
        assertEquals(0, segunda.getFallidas());
        assertEquals(2, peliculaDAO.listar().size(), "no se duplicó ninguna");
    }

    /**
     * Descartar una vez tiene que alcanzar: si la descartada no contara como cargada, la
     * corrida siguiente la volvería a proponer y habría que descartarla todas las noches.
     */
    @Test
    void unaDescartadaTampocoSeVuelveAProponer() {
        catalogo.queTraiga("Duna");
        gestor.ejecutar(1);
        revision.descartar(peliculaDAO.listar().get(0).getId());

        Importacion segunda = gestor.ejecutar(1);

        assertEquals(0, segunda.getNuevas());
        assertEquals(1, segunda.getSalteadas());
    }

    /** TMDB puede traer el mismo título dos veces entre páginas. No es una falla. */
    @Test
    void elMismoTituloDosVecesEnLaCorridaCuentaComoSalteado() {
        catalogo.queTraiga("Duna", "Duna");

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(1, importacion.getNuevas());
        assertEquals(1, importacion.getSalteadas());
        assertEquals(0, importacion.getFallidas());
    }

    /**
     * Una película que el alta rechaza no corta la corrida: queda anotada y se sigue con la
     * siguiente. Una corrida que se cae a la mitad es peor que una que no corre.
     */
    @Test
    void laQueElAltaRechazaQuedaFallidaYLaCorridaSigue() {
        catalogo.queTraiga(sinDuracion("Corto de festival"), pelicula("Duna"));

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.TERMINADA, importacion.getEstado());
        assertEquals(1, importacion.getNuevas());
        assertEquals(1, importacion.getFallidas());
        assertTrue(importacion.getDetalle().contains("✗ Corto de festival"),
                "el motivo tiene que quedar en el detalle: " + importacion.getDetalle());
        assertEquals(1, peliculaDAO.listar().size(), "la buena entró igual");
    }

    @Test
    void elDetalleNombraLoQueEntroConSuId() {
        catalogo.queTraiga("Duna");

        Importacion importacion = gestor.ejecutar(1);

        int id = peliculaDAO.listar().get(0).getId();
        assertEquals("+ [" + id + "] Duna", importacion.getDetalle());
    }

    @Test
    void sinPaginasSeTraeUna() {
        assertEquals(1, gestor.ejecutar(null).getPaginas());
        assertEquals(1, catalogo.paginasPedidas());
    }

    @Test
    void masDeTresPaginasSeRechazaYNiSiquieraSeLlamaAlCatalogo() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> gestor.ejecutar(4));

        assertEquals("Las páginas a importar van de 1 a 3", error.getMessage());
        assertEquals(0, catalogo.consultas());
        assertTrue(gestor.listar().isEmpty(), "no tendría que haber quedado registro");
    }

    /**
     * Que el catálogo externo no conteste no es un error del sistema: es un resultado. La
     * corrida queda anotada como fallida con el motivo y el encargado lo ve en el historial.
     */
    @Test
    void siElCatalogoFallaLaCorridaQuedaFallidaConElMotivo() {
        catalogo.queFalleCon("TMDB rechazó el token: revisá TMDB_TOKEN");

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.FALLIDA, importacion.getEstado());
        assertEquals("TMDB rechazó el token: revisá TMDB_TOKEN", importacion.getDetalle());
        assertEquals(0, importacion.getNuevas());
    }

    @Test
    void elHistorialVieneDeLaMasNuevaALaMasVieja() {
        catalogo.queTraiga("Duna");
        gestor.ejecutar(1);
        catalogo.queTraiga("Vaiana", "Wicked");
        gestor.ejecutar(2);

        assertEquals(2, gestor.listar().size());
        assertEquals(2, gestor.listar().get(0).getNuevas());
        assertEquals(1, gestor.listar().get(1).getNuevas());
    }

    @Test
    void noSePuedePedirOtraMientrasHayUnaEnCurso() {
        importacionDAO.guardar(new ImportacionImpl(1, LocalDateTime.now()));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> gestor.ejecutar(1));

        assertEquals("Ya hay una importación en curso: esperá a que termine", error.getMessage());
        assertEquals(0, catalogo.consultas());
    }

    @Test
    void dosCorridasSeguidasSeRechazanSiHayEsperaConfigurada() {
        GestorImportaciones conEspera = new GestorImportaciones(importacionDAO, catalogo,
                cartelera, revision, Duration.ofMinutes(5), Duration.ofMinutes(1));
        conEspera.ejecutar(1);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> conEspera.ejecutar(1));

        assertEquals("El importador corrió recién: esperá 60 segundos antes de volver a pedirlo",
                error.getMessage());
        assertEquals(1, catalogo.consultas());
    }

    /**
     * Si el backend se reinicia a mitad de corrida, la fila queda EN_CURSO para siempre y
     * con ella el sistema no aceptaría una importación nunca más. La caduca el que consulta.
     */
    @Test
    void unaCorridaColgadaCaducaSolaYDesbloqueaElSistema() {
        importacionDAO.guardar(new ImportacionImpl(1, LocalDateTime.now().minusMinutes(10)));

        Importacion caducada = gestor.listar().get(0);

        assertEquals(EstadoImportacion.FALLIDA, caducada.getEstado());
        assertEquals("La corrida no terminó a tiempo. Puede haber cargado algunas películas "
                + "igual: mirá el buzón.", caducada.getDetalle());
        assertEquals(EstadoImportacion.TERMINADA, gestor.ejecutar(1).getEstado());
    }

    @Test
    void unaCorridaEnCursoRecienPedidaNoCaduca() {
        importacionDAO.guardar(new ImportacionImpl(1, LocalDateTime.now()));

        assertEquals(EstadoImportacion.EN_CURSO, gestor.listar().get(0).getEstado());
    }

    @Test
    void elEstadoDelImportadorSePasaTalCual() {
        catalogo.queEste(false, "Falta el token de TMDB");

        assertFalse(gestor.estadoDelImportador().disponible());
        assertEquals("Falta el token de TMDB", gestor.estadoDelImportador().detalle());
    }

    /** Lo que TMDB trae sin duración: el alta lo rechaza por R2, y así tiene que ser. */
    private static DatosPelicula sinDuracion(String titulo) {
        return new DatosPelicula(titulo, 0, List.of(Genero.DRAMA), Clasificacion.ATP,
                "", "", 2026, "Inglés", "", false, 6.0, 50);
    }
}
