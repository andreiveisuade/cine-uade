package ar.uade.cine.controller.controladores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ar.uade.cine.PruebaDeApi;
import ar.uade.cine.infrastructure.importador.CatalogoDePrueba;

/**
 * El doble clic sobre el botón de importar.
 *
 * <p>Vive en su propia clase porque necesita lo contrario que el resto de las pruebas del
 * importador: la espera entre corridas puesta. El perfil de test la deja en cero para que
 * ninguna prueba tenga que dormir un minuto, así que acá se la vuelve a declarar —y eso
 * levanta un contexto aparte, que es el precio de probar una configuración distinta.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "cine.importador.espera-entre-corridas=60s")
class EsperaEntreImportacionesTest extends PruebaDeApi {

    @Autowired
    private CatalogoDePrueba catalogo;

    /**
     * El segundo pedido no llega a TMDB: la cartelera no cambió en veinte segundos y cada
     * corrida son sesenta llamadas contra una cuota.
     */
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
