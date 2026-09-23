package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

/**
 * Clase aparte porque necesita la espera entre corridas puesta, que el perfil de test deja en
 * cero; eso levanta un contexto propio.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "cine.importador.espera-entre-corridas=60s")
class EsperaEntreImportacionesTest extends PruebaDeApi {

    @Autowired
    private CatalogoDePrueba catalogo;

    /** Cada corrida son sesenta llamadas contra la cuota de TMDB. */
    @Test
    void apretarDosVecesSeguidoNoCorreDosVeces() {
        post("/api/importaciones", "{}");

        Respuesta segunda = post("/api/importaciones", "{}");

        assertEquals(400, segunda.estado());
        assertEquals("El importador corrió recién: esperá 60 segundos antes de volver a pedirlo",
                segunda.error());
        assertEquals(1, catalogo.consultas());
    }
}
