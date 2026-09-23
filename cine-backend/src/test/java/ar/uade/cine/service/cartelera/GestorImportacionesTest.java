package ar.uade.cine.service.cartelera;

import static ar.uade.cine.infrastructure.importador.CatalogoDePrueba.pelicula;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.uade.cine.PruebaDeIntegracion;
import ar.uade.cine.repository.ImportacionRepository;
import ar.uade.cine.repository.PeliculaRepository;
import ar.uade.cine.model.cartelera.Clasificacion;
import ar.uade.cine.model.cartelera.EstadoImportacion;
import ar.uade.cine.model.cartelera.EstadoRevision;
import ar.uade.cine.model.cartelera.Genero;
import ar.uade.cine.model.cartelera.Importacion;
import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;
import ar.uade.cine.service.funciones.GestorFunciones;
import ar.uade.cine.service.programaciones.GestorProgramaciones;

/**
 * Contra un catálogo de mentira: qué entra al buzón, qué se saltea y qué queda registrado.
 * El alta es real, por {@link GestorRevisionCartelera}, con las mismas reglas que a mano.
 */
class GestorImportacionesTest extends PruebaDeIntegracion {

    @Autowired
    private ImportacionRepository importacionRepository;
    @Autowired
    private PeliculaRepository peliculaRepository;
    @Autowired
    private CatalogoDePrueba catalogo;
    @Autowired
    private GestorCartelera cartelera;
    @Autowired
    private GestorRevisionCartelera revision;

    /**
     * El bean y no un {@code new}: sin el proxy transaccional no se ven los errores de límite
     * de transacción. El perfil de test pone la espera entre corridas en cero.
     */
    @Autowired
    private GestorImportaciones gestor;

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

    /** Lo que baja de un catálogo ajeno es una propuesta, no una decisión del cine. */
    @Test
    void loQueEntraQuedaEnElBuzonYNoEnCartelera() {
        catalogo.queTraiga("Duna");

        gestor.ejecutar(1);

        Pelicula importada = peliculaRepository.findAll().get(0);
        assertEquals(EstadoRevision.PENDIENTE, importada.getEstadoRevision());
        assertFalse(importada.estaEnCartelera(), "no puede ofrecerse antes de que la miren");
    }

    /** La corrida es repetible: R1 rechazaría los títulos que ya están. */
    @Test
    void lasQueYaEstanNoSeVuelvenAProponer() {
        catalogo.queTraiga("Duna", "Vaiana");
        gestor.ejecutar(1);

        Importacion segunda = gestor.ejecutar(1);

        assertEquals(0, segunda.getNuevas());
        assertEquals(2, segunda.getSalteadas());
        assertEquals(0, segunda.getFallidas());
        assertEquals(2, peliculaRepository.findAll().size(), "no se duplicó ninguna");
    }

    /** Si la descartada no contara como cargada, cada corrida la volvería a proponer. */
    @Test
    void unaDescartadaTampocoSeVuelveAProponer() {
        catalogo.queTraiga("Duna");
        gestor.ejecutar(1);
        revision.descartar(peliculaRepository.findAll().get(0).getId());

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

    /** Una corrida que se cae a la mitad es peor que una que no corre. */
    @Test
    void laQueElAltaRechazaQuedaFallidaYLaCorridaSigue() {
        catalogo.queTraiga(sinDuracion("Corto de festival"), pelicula("Duna"));

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.TERMINADA, importacion.getEstado());
        assertEquals(1, importacion.getNuevas());
        assertEquals(1, importacion.getFallidas());
        assertTrue(importacion.getDetalle().contains("✗ Corto de festival"),
                "el motivo tiene que quedar en el detalle: " + importacion.getDetalle());
        assertEquals(1, peliculaRepository.findAll().size(), "la buena entró igual");
    }

    @Test
    void elDetalleNombraLoQueEntroConSuId() {
        catalogo.queTraiga("Duna");

        Importacion importacion = gestor.ejecutar(1);

        int id = peliculaRepository.findAll().get(0).getId();
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

    /** Que el catálogo no conteste es un resultado, no un error: queda en el historial. */
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
        importacionRepository.save(new Importacion(1, reloj.ahora()));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> gestor.ejecutar(1));

        assertEquals("Ya hay una importación en curso: esperá a que termine", error.getMessage());
        assertEquals(0, catalogo.consultas());
    }

    @Test
    void dosCorridasSeguidasSeRechazanSiHayEsperaConfigurada() {
        GestorImportaciones conEspera = new GestorImportaciones(importacionRepository, catalogo,
                cartelera, revision, Duration.ofMinutes(5), Duration.ofMinutes(1), reloj);
        conEspera.ejecutar(1);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> conEspera.ejecutar(1));

        assertEquals("El importador corrió recién: esperá 60 segundos antes de volver a pedirlo",
                error.getMessage());
        assertEquals(1, catalogo.consultas());
    }

    /** Un reinicio a mitad de corrida deja la fila EN_CURSO para siempre; la caduca quien consulta. */
    @Test
    void unaCorridaColgadaCaducaSolaYDesbloqueaElSistema() {
        importacionRepository.save(new Importacion(1, reloj.ahora().minusMinutes(10)));

        Importacion caducada = gestor.listar().get(0);

        assertEquals(EstadoImportacion.FALLIDA, caducada.getEstado());
        assertEquals("La corrida no terminó a tiempo. Puede haber cargado algunas películas "
                + "igual: mirá el buzón.", caducada.getDetalle());
        assertEquals(EstadoImportacion.TERMINADA, gestor.ejecutar(1).getEstado());
    }

    @Test
    void unaCorridaEnCursoRecienPedidaNoCaduca() {
        importacionRepository.save(new Importacion(1, reloj.ahora()));

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
