package ar.uade.cine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/ar/uade/cine");

    // Romper una regla es cambiar esta tabla a mano, a propósito.
    private static final Map<String, Set<String>> PERMITIDO = Map.of(
            "model", Set.of("model"),
            "dto", Set.of("model", "dto"),
            "repository", Set.of("model", "repository"),
            "infrastructure", Set.of("model", "repository", "infrastructure", "service"),
            "service", Set.of("model", "repository", "infrastructure", "service"),
            "controller", Set.of("model", "dto", "repository", "infrastructure", "service",
                    "controller"));

    @Nested
    @DisplayName("Las flechas entre capas van todas para el mismo lado")
    class Dependencias {

        @Test
        @DisplayName("model/ no importa ninguna otra capa")
        void elModeloNoDependeDeNada() {
            assertSinViolaciones(violacionesDeCapa("model"));
        }

        @Test
        @DisplayName("repository/ conoce el modelo que guarda, y nada más")
        void laPersistenciaSoloConoceElDominio() {
            assertSinViolaciones(violacionesDeCapa("repository"));
        }

        @Test
        @DisplayName("service/ no importa controller/ ni dto/: las reglas no saben que existe HTTP")
        void elServicioNoDependeDeLaEntrada() {
            assertSinViolaciones(violacionesDeCapa("service"));
        }

        @Test
        @DisplayName("dto/ no tiene lógica: no importa service/ ni repository/")
        void losDtoNoDependenDeLosGestores() {
            assertSinViolaciones(violacionesDeCapa("dto"));
        }

        @Test
        @DisplayName("infrastructure/ es adaptador de salida, no llama a la entrada")
        void laInfraestructuraNoDependeDeLaApi() {
            // 'service' porque comprobantes/ formatea el record Bordero, un dato de salida.
            assertSinViolaciones(violacionesDeCapa("infrastructure"));
        }
    }

    @Nested
    @DisplayName("Inversión de dependencias: un solo lugar elige la implementación")
    class ImplementacionesConcretas {

        @Test
        @DisplayName("ningún servicio habla con la base por abajo del repositorio")
        void nadieSeSalteaElRepositorio() {
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : fuentes()) {
                String capa = capaDe(archivo);
                if (!capa.equals("service") && !capa.equals("controller")) {
                    continue;
                }
                for (String importado : importsExternos(archivo)) {
                    if (importado.startsWith("org.springframework.jdbc")
                            || importado.startsWith("jakarta.persistence")
                            || importado.startsWith("java.sql")) {
                        violaciones.add(RAIZ.relativize(archivo) + " importa " + importado);
                    }
                }
            }
            assertSinViolaciones(violaciones);
        }
    }

    private static List<String> violacionesDeCapa(String capa) {
        List<String> violaciones = new ArrayList<>();
        for (Path archivo : fuentes()) {
            if (!capaDe(archivo).equals(capa)) {
                continue;
            }
            for (String importado : importsInternos(archivo)) {
                String destino = importado.split("\\.")[0];
                if (!PERMITIDO.get(capa).contains(destino)) {
                    violaciones.add(RAIZ.relativize(archivo) + " importa " + destino + "/");
                }
            }
        }
        return violaciones;
    }

    private static String capaDe(Path archivo) {
        Path relativo = RAIZ.relativize(archivo);
        return relativo.getNameCount() == 1 ? "" : relativo.getName(0).toString();
    }

    private static List<String> importsInternos(Path archivo) {
        try {
            return Files.readAllLines(archivo).stream()
                    .map(String::strip)
                    .filter(linea -> linea.startsWith("import "))
                    .map(linea -> linea.replaceFirst("^import (static )?", "").replaceFirst(";$", ""))
                    .filter(clase -> clase.startsWith("ar.uade.cine."))
                    .map(clase -> clase.substring("ar.uade.cine.".length()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> importsExternos(Path archivo) {
        try {
            return Files.readAllLines(archivo).stream()
                    .map(String::strip)
                    .filter(linea -> linea.startsWith("import "))
                    .map(linea -> linea.replaceFirst("^import (static )?", "").replaceFirst(";$", ""))
                    .filter(clase -> !clase.startsWith("ar.uade.cine."))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> fuentes() {
        try (Stream<Path> archivos = Files.walk(RAIZ)) {
            return archivos.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertSinViolaciones(List<String> violaciones) {
        assertTrue(violaciones.isEmpty(),
                () -> "La capa quedó al revés en:\n  " + String.join("\n  ", violaciones));
    }
}
