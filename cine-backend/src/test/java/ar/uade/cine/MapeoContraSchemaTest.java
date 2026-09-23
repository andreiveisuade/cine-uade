package ar.uade.cine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Crea la base con el {@code schema.sql} real y deja que Hibernate valide contra ella: el
 * resto de la suite genera las tablas desde las entidades y no ve un desacuerdo. No afirma
 * nada a propósito: la prueba es que el contexto levante.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema;MODE=MySQL;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'src/main/resources/schema.sql'",
        "spring.jpa.hibernate.ddl-auto=validate",
        // El INIT de H2 corre el script en cada conexión que se abre.
        "spring.datasource.hikari.maximum-pool-size=1"
})
@ActiveProfiles("test")
class MapeoContraSchemaTest {

    @Test
    void lasEntidadesCoincidenConElSchemaDesplegado() {
    }
}
