package ar.uade.cine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Sin asserts a propósito: la prueba es que el contexto levante contra el schema.sql real,
// en una base aparte y sin otro test que la toque antes.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema;MODE=MySQL;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'src/main/resources/db/schema.sql'",
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
