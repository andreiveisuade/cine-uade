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

// RESTART IDENTITY: los tests asumen que el primer id es 1.
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

        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tablas.forEach(tabla -> jdbc.execute("TRUNCATE TABLE " + tabla + " RESTART IDENTITY"));
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");

        borrar(Path.of("target/comprobantes"));
    }

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
