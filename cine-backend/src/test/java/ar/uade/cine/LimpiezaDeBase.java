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
 * Deja la base como recién creada entre un test y el siguiente.
 *
 * <p>Hace falta porque el contexto de Spring se comparte entre las clases de test —es lo
 * que hace que la suite tarde segundos y no minutos— y con él la base H2. Sin esto, la
 * película que crea un test aparecería en el siguiente.
 *
 * <p>{@code RESTART IDENTITY} es la parte que no se puede omitir: borrar las filas no
 * reinicia el contador de {@code AUTO_INCREMENT}, y los tests se escribieron sabiendo que
 * la primera película que dan de alta es la número uno. Un {@code deleteAll} de los
 * repositorios dejaría la base vacía pero la numeración corrida.
 *
 * <p>Las tablas se leen del catálogo en vez de listarlas acá: una entidad nueva no tiene que
 * acordarse de sumarse a una lista, que es la clase de olvido que se descubre como un test
 * que falla sin motivo aparente.
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

        // Sin desactivar las claves foráneas habría que truncar en el orden exacto de las
        // dependencias, y ese orden cambia cada vez que se agrega una relación.
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tablas.forEach(tabla -> jdbc.execute("TRUNCATE TABLE " + tabla + " RESTART IDENTITY"));
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");

        borrar(Path.of("target/comprobantes"));
    }

    /**
     * Los comprobantes también, y por el mismo motivo que las tablas: los ids vuelven a
     * empezar en uno, así que el ticket de una corrida anterior queda con el nombre que va a
     * buscar el test siguiente y le hace creer que se emitió.
     */
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
