package ar.uade.cine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Las entidades contra el schema que de verdad corre en MySQL.
 *
 * <p>El resto de la suite deja que Hibernate cree las tablas desde las entidades, así que
 * nunca puede detectar el desacuerdo entre las dos: si una entidad dice {@code posterUrl} y
 * la columna se llama {@code poster_url}, la base de los tests se genera con el nombre de la
 * entidad y todo pasa. En producción eso es un arranque fallido.
 *
 * <p>Acá se hace al revés: la base se crea corriendo {@code schema.sql} —el mismo archivo que
 * el compose le monta a MySQL— y recién después Hibernate valida contra ella. Es lo que hace
 * que {@code ddl-auto: validate} no sea una sorpresa que se descubre al desplegar.
 *
 * <p>No afirma nada en el cuerpo a propósito: la prueba es que el contexto levante. Si una
 * entidad y su tabla no coinciden, el arranque falla con el nombre de la columna que sobra o
 * falta.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema;MODE=MySQL;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'src/main/resources/schema.sql'",
        "spring.jpa.hibernate.ddl-auto=validate",
        // Una sola conexión: el INIT de H2 corre el script en cada una que se abre, y con
        // el pool entero son diez ejecuciones de las diecinueve tablas.
        "spring.datasource.hikari.maximum-pool-size=1"
})
@ActiveProfiles("test")
class MapeoContraSchemaTest {

    @Test
    void lasEntidadesCoincidenConElSchemaDesplegado() {
    }
}
