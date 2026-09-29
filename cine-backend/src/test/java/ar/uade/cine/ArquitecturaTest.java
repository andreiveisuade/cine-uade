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
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.uade.cine.model.rechazos.ConflictoDeNegocio;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.rechazos.RecursoNoEncontrado;

class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/ar/uade/cine");

    // Romper una regla es cambiar esta tabla a mano, a propósito.
    private static final Map<String, Set<String>> PERMITIDO = Map.of(
            "model", Set.of("model"),
            "dto", Set.of("model", "dto"),
            "repository", Set.of("model", "repository"),
            "infrastructure", Set.of("model", "repository", "infrastructure", "service"),
            "service", Set.of("model", "repository", "infrastructure", "service"),
            "controller", Set.of("model", "dto", "infrastructure", "service", "controller"));

    // Los adaptadores concretos: el resto depende del puerto y Adaptadores elige cuál va.
    private static final List<String> IMPLEMENTACIONES = List.of(
            "infrastructure.comprobantes.txt",
            "infrastructure.importador.tmdb",
            "infrastructure.pasarelas.emulada");

    @Nested
    @DisplayName("Las flechas entre capas van todas para el mismo lado")
    class Dependencias {

        @ParameterizedTest(name = "{1}")
        @CsvSource(textBlock = """
                model,          'model/ no importa ninguna otra capa'
                repository,     'repository/ conoce el modelo que guarda, y nada más'
                service,        'service/ no importa controller/ ni dto/: las reglas no saben que existe HTTP'
                dto,            'dto/ no tiene lógica: no importa service/ ni repository/'
                # 'service' porque importador/ devuelve DatosPelicula y seguridad/ re-hashea claves por GestorEmpleados.
                infrastructure, 'infrastructure/ es adaptador de salida, no llama a la entrada'
                controller,     'controller/ no toca la base: habla con los gestores'
                """)
        void cadaCapaImportaSoloLasQueTieneDebajo(String capa, String regla) {
            assertSinViolaciones(violacionesDeCapa(capa));
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

        @Test
        @DisplayName("fuera de infrastructure/ nadie nombra una implementación concreta, solo su puerto")
        void fueraDeInfrastructureSoloSeVenLosPuertos() {
            assertSinViolaciones(importsDeImplementaciones(
                    archivo -> !capaDe(archivo).equals("infrastructure")));
        }

        @Test
        @DisplayName("dentro de infrastructure/ solo Adaptadores nombra una implementación concreta")
        void soloAdaptadoresEligeLaImplementacion() {
            Path adaptadores = RAIZ.resolve("infrastructure/Adaptadores.java");
            assertSinViolaciones(importsDeImplementaciones(
                    archivo -> capaDe(archivo).equals("infrastructure") && !archivo.equals(adaptadores)));
        }

        // Lo que configura o implementa un adaptador va en la subcarpeta de ese adaptador
        // (comprobantes/txt, importador/tmdb). seguridad/ no es un puerto: configura Spring Security.
        @ParameterizedTest(name = "infrastructure/{0}/")
        @ValueSource(strings = {"comprobantes", "importador", "pasarelas", "reloj"})
        @DisplayName("la raíz de cada puerto tiene solo su contrato: la interfaz y su excepción")
        void laRaizDeCadaPuertoTieneSoloSuContrato(String puerto) {
            List<String> ajenos;
            try (Stream<Path> archivos = Files.list(RAIZ.resolve("infrastructure").resolve(puerto))) {
                ajenos = archivos.filter(archivo -> archivo.toString().endsWith(".java"))
                        .filter(archivo -> !esContrato(archivo))
                        .map(archivo -> RAIZ.relativize(archivo).toString())
                        .toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            assertTrue(ajenos.isEmpty(), () -> "No son parte del contrato del puerto: movelos a la subcarpeta "
                    + "de su adaptador:\n  " + String.join("\n  ", ajenos));
        }

        private static boolean esContrato(Path archivo) {
            String fuente = String.join("\n", lineasDe(archivo));
            return fuente.contains("public interface ") || fuente.contains(" extends RuntimeException");
        }
    }

    @Nested
    @DisplayName("Los rechazos son explícitos")
    class Rechazos {

        // ManejadorErrores contesta una IllegalArgumentException suelta con 500: la puede tirar una
        // librería con un texto técnico. Lo que el usuario tiene que leer va por un Rechazo.
        @Test
        @DisplayName("nadie tira una IllegalArgumentException suelta, sino un Rechazo de model/rechazos")
        void nadieTiraUnaIllegalArgumentExceptionSuelta() {
            List<String> violaciones = lineasQueDicen("new IllegalArgumentException(", archivo -> true);
            assertTrue(violaciones.isEmpty(), () -> "Tiran new IllegalArgumentException(…), que sale como 500: "
                    + "usá DatoInvalido (400), RecursoNoEncontrado (404) o ConflictoDeNegocio (409) "
                    + "de model/rechazos en:\n  " + String.join("\n  ", violaciones));
        }
    }

    @Nested
    @DisplayName("Cada endpoint contesta una sola forma")
    class Respuestas {

        // Con ResponseEntity<Object> un mismo endpoint contestaba el DTO o el literal null, según el caso.
        @Test
        @DisplayName("ningún controller contesta ResponseEntity<Object>")
        void ningunControllerContestaObject() {
            List<String> violaciones = lineasQueDicen("ResponseEntity<Object>",
                    archivo -> capaDe(archivo).equals("controller"));
            assertTrue(violaciones.isEmpty(), () -> "Contestan ResponseEntity<Object>: usá el DTO, una lista "
                    + "para un filtro o un rechazo para lo que no está, en:\n  " + String.join("\n  ", violaciones));
        }
    }

    // Archivo y número de línea, sin contar comentarios.
    private static List<String> lineasQueDicen(String texto, Predicate<Path> revisar) {
        List<String> encontradas = new ArrayList<>();
        for (Path archivo : fuentes()) {
            if (!revisar.test(archivo)) {
                continue;
            }
            List<String> lineas = lineasDe(archivo);
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i).strip();
                if (!linea.startsWith("//") && linea.contains(texto)) {
                    encontradas.add(RAIZ.relativize(archivo) + ":" + (i + 1));
                }
            }
        }
        return encontradas;
    }

    private static List<String> lineasDe(Path archivo) {
        try {
            return Files.readAllLines(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Su propio paquete sí la nombra: ComprobanteTxt es la base de los tres Generador*Txt.
    private static List<String> importsDeImplementaciones(Predicate<Path> revisar) {
        List<String> violaciones = new ArrayList<>();
        for (Path archivo : fuentes()) {
            if (!revisar.test(archivo)) {
                continue;
            }
            for (String importado : importsInternos(archivo)) {
                for (String implementacion : IMPLEMENTACIONES) {
                    boolean propia = archivo.startsWith(RAIZ.resolve(implementacion.replace('.', '/')));
                    if (importado.startsWith(implementacion + ".") && !propia) {
                        violaciones.add(RAIZ.relativize(archivo) + " importa " + importado);
                    }
                }
            }
        }
        return violaciones;
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
