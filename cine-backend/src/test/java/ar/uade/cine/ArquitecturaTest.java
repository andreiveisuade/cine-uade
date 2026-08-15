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

/**
 * Las capas, escritas de manera que no puedan mentir.
 *
 * <p>Que {@code dominio/} no dependa de nadie y que la implementación concreta de cada DAO
 * se elija en un solo lugar son las dos afirmaciones que sostienen el manual y los diagramas
 * de SOLID del TP. Hasta acá eran ciertas por disciplina: nada impedía que un import apurado
 * las rompiera y que el diagrama quedara describiendo un sistema que ya no existe. Este test
 * las lee del código fuente en cada {@code mvn test}.
 *
 * <p>No hace falta ninguna dependencia nueva: alcanza con leer los {@code import} de cada
 * archivo, porque es exactamente lo que se quiere restringir.
 */
class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/ar/uade/cine");

    /**
     * Hacia dónde puede importar cada capa. La lectura es "de arriba hacia abajo": una capa
     * sólo puede nombrar a las que tiene debajo, nunca a las de arriba, y por eso
     * {@code servicio} no incluye ni {@code api} ni {@code dto} (las reglas de negocio no
     * saben que existe HTTP) y {@code dominio} no incluye nada (no sabe siquiera que se lo
     * guarda en algún lado).
     */
    private static final Map<String, Set<String>> PERMITIDO = Map.of(
            "dominio", Set.of("dominio"),
            "dto", Set.of("dominio", "dto"),
            "persistencia", Set.of("dominio", "persistencia"),
            "infraestructura", Set.of("dominio", "persistencia", "infraestructura", "servicio"),
            "servicio", Set.of("dominio", "persistencia", "infraestructura", "servicio"),
            "api", Set.of("dominio", "dto", "persistencia", "infraestructura", "servicio", "api"));

    @Nested
    @DisplayName("Las flechas entre capas van todas para el mismo lado")
    class Dependencias {

        @Test
        @DisplayName("dominio/ no importa ninguna otra capa")
        void elDominioNoDependeDeNada() {
            assertSinViolaciones(violacionesDeCapa("dominio"));
        }

        @Test
        @DisplayName("persistencia/ conoce el dominio que guarda, y nada más")
        void laPersistenciaSoloConoceElDominio() {
            assertSinViolaciones(violacionesDeCapa("persistencia"));
        }

        @Test
        @DisplayName("servicio/ no importa api/ ni dto/: las reglas no saben que existe HTTP")
        void elServicioNoDependeDeLaEntrada() {
            assertSinViolaciones(violacionesDeCapa("servicio"));
        }

        @Test
        @DisplayName("dto/ no tiene lógica: no importa servicio/ ni persistencia/")
        void losDtoNoDependenDeLosGestores() {
            assertSinViolaciones(violacionesDeCapa("dto"));
        }

        @Test
        @DisplayName("infraestructura/ es adaptador de salida, no llama a la entrada")
        void laInfraestructuraNoDependeDeLaApi() {
            // La excepción viva es comprobantes/, que importa el record servicio.informes.Bordero
            // para poder formatearlo. Es un dato de salida, no un gestor; por eso 'servicio'
            // está permitido acá. Si algún día un adaptador importara un Gestor*, esta regla
            // habría que ajustarla a mano, y esa discusión es justamente lo que se quiere forzar.
            assertSinViolaciones(violacionesDeCapa("infraestructura"));
        }
    }

    @Nested
    @DisplayName("Inversión de dependencias: un solo lugar elige la implementación")
    class ImplementacionesConcretas {

        @Test
        @DisplayName("sólo Aplicacion nombra un DAO de mysql/, memoria/, archivo/ o redis/")
        void laImplementacionSeEligeEnUnSoloLugar() {
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : fuentes()) {
                if (archivo.endsWith("Aplicacion.java")) {
                    continue;
                }
                for (String importado : importsInternos(archivo)) {
                    if (importado.matches("persistencia\\.(mysql|memoria|archivo|redis)\\..*")) {
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

    /** La capa es la primera carpeta bajo {@code ar/uade/cine/}. Aplicacion.java no tiene. */
    private static String capaDe(Path archivo) {
        Path relativo = RAIZ.relativize(archivo);
        return relativo.getNameCount() == 1 ? "" : relativo.getName(0).toString();
    }

    /** Los {@code import ar.uade.cine.X} de un archivo, ya sin el prefijo del paquete. */
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
