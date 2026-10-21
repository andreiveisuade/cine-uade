package ar.uade.cine.swing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Las pantallas no conocen la librería de calendario: la usan a través de CampoFecha y Calendario (comun/). */
class ArquitecturaTest {

    private static final Path PANTALLAS = Path.of("src/main/java/ar/uade/cine/swing/pantallas");

    @Test
    void ningunaPantallaImportaJCalendar() throws IOException {
        try (Stream<Path> archivos = Files.walk(PANTALLAS)) {
            List<String> infractores = archivos.filter(p -> p.toString().endsWith(".java"))
                    .filter(ArquitecturaTest::importaJCalendar)
                    .map(p -> PANTALLAS.relativize(p).toString())
                    .toList();
            assertEquals(List.of(), infractores);
        }
    }

    private static boolean importaJCalendar(Path archivo) {
        try {
            return Files.readString(archivo).contains("import com.toedter");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
