package ar.uade.cine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deja la base como recién creada entre tests, porque el contexto (y con él H2) se comparte.
 * {@code RESTART IDENTITY} es imprescindible: los tests asumen que el primer id es 1. Las
 * tablas se leen del catálogo para que una entidad nueva no tenga que anotarse en una lista.
 */
@Component
@Profile("test")
public class LimpiezaDeBase {

    private final JdbcTemplate jdbc;

    public LimpiezaDeBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void limpiar() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'",
                String.class);

        // Sin esto habría que truncar en el orden de las claves foráneas.
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tablas.forEach(tabla -> jdbc.execute("TRUNCATE TABLE " + tabla + " RESTART IDENTITY"));
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");

        borrar(Path.of("target/comprobantes"));
    }

    /** Los ids vuelven a 1: un ticket viejo haría creer al test siguiente que se emitió. */
    private static void borrar(Path directorio) {
        if (!Files.exists(directorio)) {
            return;
        }
        try (var archivos = Files.walk(directorio)) {
            archivos.sorted(Comparator.reverseOrder()).forEach(archivo -> {
                try {
                    Files.delete(archivo);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
