package ar.uade.cine.infrastructure.bloqueos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El único lugar donde se ejecutan de verdad los comandos de Redis. Todo lo demás
 * —{@link ar.uade.cine.service.ventas.OcupacionTest}— corre contra la implementación en memoria,
 * así que los dos scripts Lua de esta clase no tendrían ninguna cobertura sin este test.
 *
 * <p><strong>Se saltea salvo que se le diga contra qué Redis correr.</strong> No usa el
 * puerto por defecto a propósito: escribiría en el Redis que la máquina tenga levantado,
 * que puede ser el de otro proyecto. Hay que darle uno descartable:
 *
 * <pre>
 * docker run -d --rm --name cine-redis-prueba -p 6399:6379 redis:8-alpine
 * REDIS_TEST_PORT=6399 mvn test -Dtest=BloqueoButacasRedisTest
 * docker stop cine-redis-prueba
 * </pre>
 *
 * <p>Que se saltee y no falle es lo que mantiene la regla de que {@code mvn test} corre
 * solo, sin levantar nada.
 */
class BloqueoButacasRedisTest {

    /** Una función que no existe en ninguna base: las claves son solo de este test. */
    private static final int FUNCION = 99;

    private BloqueoButacasRedis bloqueos;

    @BeforeEach
    void pedirUnRedisDescartable() {
        String puerto = System.getenv("REDIS_TEST_PORT");
        assumeTrue(puerto != null && !puerto.isBlank(),
                "sin REDIS_TEST_PORT no hay contra qué correr: ver el javadoc de la clase");
        bloqueos = new BloqueoButacasRedis("127.0.0.1", Integer.parseInt(puerto));
        bloqueos.bloqueadas(FUNCION).forEach((asiento, sesion) ->
                bloqueos.liberar(FUNCION, asiento, sesion));
    }

    @Test
    void laPrimeraSeLaLlevaYLaPuedeRenovar() {
        assertTrue(bloqueos.bloquear(FUNCION, 5, "ana", Duration.ofSeconds(30)));
        assertFalse(bloqueos.bloquear(FUNCION, 5, "beto", Duration.ofSeconds(30)));
        assertTrue(bloqueos.bloquear(FUNCION, 5, "ana", Duration.ofSeconds(30)),
                "volver a pedirla renueva en vez de fallar");
    }

    @Test
    void listaQuienTieneCadaButacaYNoMezclaFunciones() {
        bloqueos.bloquear(FUNCION, 5, "ana", Duration.ofSeconds(30));
        bloqueos.bloquear(FUNCION, 6, "beto", Duration.ofSeconds(30));

        assertEquals(Map.of(5, "ana", 6, "beto"), bloqueos.bloqueadas(FUNCION));
        assertEquals(Map.of(), bloqueos.bloqueadas(FUNCION + 1));
    }

    @Test
    void soltarNoSirveParaSoltarLaDeOtro() {
        bloqueos.bloquear(FUNCION, 5, "ana", Duration.ofSeconds(30));

        bloqueos.liberar(FUNCION, 5, "beto");
        assertEquals(Map.of(5, "ana"), bloqueos.bloqueadas(FUNCION));

        bloqueos.liberar(FUNCION, 5, "ana");
        assertEquals(Map.of(), bloqueos.bloqueadas(FUNCION));
    }

    /** El vencimiento lo lleva Redis con el TTL de la clave: acá sí hay que esperarlo. */
    @Test
    void laClaveVenceSola() throws InterruptedException {
        bloqueos.bloquear(FUNCION, 7, "ana", Duration.ofMillis(300));

        Thread.sleep(500);

        assertEquals(Map.of(), bloqueos.bloqueadas(FUNCION));
        assertTrue(bloqueos.bloquear(FUNCION, 7, "beto", Duration.ofSeconds(30)));
    }
}
