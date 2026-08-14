package ar.uade.cine.servicio.cartelera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ar.uade.cine.dominio.cartelera.EstadoImportacion;
import ar.uade.cine.dominio.cartelera.Importacion;
import ar.uade.cine.dominio.cartelera.ImportacionImpl;
import ar.uade.cine.importador.ImportadorDePrueba;
import ar.uade.cine.persistencia.ImportacionDAO;
import ar.uade.cine.persistencia.memoria.ImportacionDAOMemoria;

/**
 * El gestor contra un importador de mentira: acá se prueba qué queda registrado y qué se
 * rechaza, no que TMDB conteste.
 *
 * <p>La espera entre corridas va en cero en el gestor de la mayoría de los tests. Es una
 * protección contra el doble clic y tiene su propio test; dejarla puesta en todos obligaría
 * a que cada uno durmiera un minuto.
 */
class GestorImportacionesTest {

    private final ImportacionDAO importacionDAO = new ImportacionDAOMemoria();
    private final ImportadorDePrueba importador = new ImportadorDePrueba();
    private final GestorImportaciones gestor = new GestorImportaciones(importacionDAO, importador,
            Duration.ofMinutes(5), Duration.ZERO);

    @Test
    void unaCorridaQueVuelveBienQuedaRegistradaConLoQueTrajo() {
        importador.queTraiga(18, 2, 0);

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.TERMINADA, importacion.getEstado());
        assertEquals(18, importacion.getNuevas());
        assertEquals(2, importacion.getSalteadas());
        assertEquals(0, importacion.getFallidas());
        assertNotNull(importacion.getTerminoEn());
        assertEquals(1, importador.corridas());
    }

    @Test
    void sinPaginasSeTraeUna() {
        assertEquals(1, gestor.ejecutar(null).getPaginas());
        assertEquals(1, importador.paginasPedidas());
    }

    @Test
    void masDeTresPaginasSeRechazaYNiSiquieraSeLlamaAlImportador() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> gestor.ejecutar(4));

        assertEquals("Las páginas a importar van de 1 a 3", error.getMessage());
        assertEquals(0, importador.corridas());
        assertTrue(gestor.listar().isEmpty(), "no tendría que haber quedado registro");
    }

    /**
     * Que el importador no esté no es un error del sistema: es un resultado. La corrida
     * queda anotada como fallida con el motivo y el encargado lo ve en el historial.
     */
    @Test
    void siElImportadorFallaLaCorridaQuedaFallidaConElMotivo() {
        importador.queFalleCon("El importador no responde en http://parser:8090");

        Importacion importacion = gestor.ejecutar(1);

        assertEquals(EstadoImportacion.FALLIDA, importacion.getEstado());
        assertEquals("El importador no responde en http://parser:8090", importacion.getDetalle());
        assertEquals(0, importacion.getNuevas());
    }

    @Test
    void elHistorialVieneDeLaMasNuevaALaMasVieja() {
        importador.queTraiga(1, 0, 0);
        gestor.ejecutar(1);
        importador.queTraiga(2, 0, 0);
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
        assertEquals(0, importador.corridas());
    }

    @Test
    void dosCorridasSeguidasSeRechazanSiHayEsperaConfigurada() {
        GestorImportaciones conEspera = new GestorImportaciones(importacionDAO, importador,
                Duration.ofMinutes(5), Duration.ofMinutes(1));
        conEspera.ejecutar(1);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> conEspera.ejecutar(1));

        assertEquals("El importador corrió recién: esperá 60 segundos antes de volver a pedirlo",
                error.getMessage());
        assertEquals(1, importador.corridas());
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
        importador.queEste(false, "no está levantado");

        assertFalse(gestor.estadoDelImportador().disponible());
        assertEquals("no está levantado", gestor.estadoDelImportador().detalle());
    }
}
