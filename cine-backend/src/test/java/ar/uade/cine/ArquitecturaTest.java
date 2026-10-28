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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ArquitecturaTest {

    private static final Path RAIZ = Path.of("src/main/java/ar/uade/cine");

    // Un nombre calificado de tipo (paquetes en minúscula y la clase): lo mismo un import que uno escrito en el código.
    private static final Pattern NOMBRE_CALIFICADO = Pattern.compile("\\b(?:[a-z]\\w*\\.)+(?:[A-Z]\\w*|\\*)");

    // Comentarios y textos no son dependencias: un string con "ar.uade.cine.x" o un // con un import no cuentan.
    private static final Pattern COMENTARIO_O_TEXTO = Pattern.compile(
            "(?s)\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\\n])*\"|'(?:\\\\.|[^'\\\\\n])+'|/\\*.*?\\*/|//[^\n]*");

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
            "infrastructure.imagenes.base",
            "infrastructure.importador.tmdb",
            "infrastructure.pasarelas.emulada",
            "infrastructure.pasarelas.mercadopago");

    @Nested
    @DisplayName("Las flechas entre capas van todas para el mismo lado")
    class Dependencias {

        @ParameterizedTest(name = "{1}")
        @CsvSource(textBlock = """
                model,          'model/ no importa ninguna otra capa'
                repository,     'repository/ conoce el modelo que guarda, y nada más'
                service,        'service/ no importa controller/ ni dto/: las reglas no saben que existe HTTP'
                dto,            'dto/ no tiene lógica: no importa service/ ni repository/'
                # 'service' porque importador/ devuelve DatosPelicula.
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
                for (String importado : referenciasExternas(archivo)) {
                    if (importado.startsWith("org.springframework.jdbc")
                            || importado.startsWith("jakarta.persistence")
                            || importado.startsWith("org.hibernate")
                            || importado.startsWith("java.sql")) {
                        violaciones.add(RAIZ.relativize(archivo) + " nombra " + importado);
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

        // Lo de un adaptador va en su subcarpeta (comprobantes/txt, importador/tmdb); seguridad/ no es un puerto.
        @ParameterizedTest(name = "infrastructure/{0}/")
        @ValueSource(strings = {"comprobantes", "imagenes", "importador", "pasarelas", "reloj"})
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

        // ManejadorErrores contesta una IllegalArgumentException suelta con 500: lo legible va por un Rechazo.
        @Test
        @DisplayName("nadie tira una IllegalArgumentException suelta, sino un Rechazo de model/rechazos")
        void nadieTiraUnaIllegalArgumentExceptionSuelta() {
            List<String> violaciones = lineasQueDicen(
                    Pattern.compile("new\\s+(?:java\\.lang\\.)?IllegalArgumentException\\s*\\("), archivo -> true);
            assertTrue(violaciones.isEmpty(), () -> "Tiran new IllegalArgumentException(…), que sale como 500: "
                    + "usá DatoInvalido (400), RecursoNoEncontrado (404) o ConflictoDeNegocio (409) "
                    + "de model/rechazos en:\n  " + String.join("\n  ", violaciones));
        }
    }

    @Nested
    @DisplayName("Cada endpoint contesta una sola forma")
    class Respuestas {

        // ResponseEntity<Object> deja que un endpoint conteste el DTO o null según el caso.
        @Test
        @DisplayName("ningún controller contesta ResponseEntity<Object>")
        void ningunControllerContestaObject() {
            List<String> violaciones = lineasQueDicen(Pattern.compile("ResponseEntity<\\s*Object\\s*>"),
                    archivo -> capaDe(archivo).equals("controller"));
            assertTrue(violaciones.isEmpty(), () -> "Contestan ResponseEntity<Object>: usá el DTO, una lista "
                    + "para un filtro o un rechazo para lo que no está, en:\n  " + String.join("\n  ", violaciones));
        }
    }

    // Archivo y número de línea, sin contar comentarios ni textos.
    private static List<String> lineasQueDicen(Pattern patron, Predicate<Path> revisar) {
        List<String> encontradas = new ArrayList<>();
        for (Path archivo : fuentes()) {
            if (!revisar.test(archivo)) {
                continue;
            }
            String[] lineas = codigoDe(archivo).split("\n", -1);
            for (int i = 0; i < lineas.length; i++) {
                if (patron.matcher(lineas[i]).find()) {
                    encontradas.add(RAIZ.relativize(archivo) + ":" + (i + 1));
                }
            }
        }
        return encontradas;
    }

    // El fuente con cada comentario y cada texto en blanco; se conservan los saltos para no correr los números de línea.
    private static String codigoDe(Path archivo) {
        Matcher ignorado = COMENTARIO_O_TEXTO.matcher(String.join("\n", lineasDe(archivo)));
        StringBuilder codigo = new StringBuilder();
        while (ignorado.find()) {
            ignorado.appendReplacement(codigo, ignorado.group().replaceAll("[^\n]", ""));
        }
        return ignorado.appendTail(codigo).toString();
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
        return referencias(archivo).stream()
                .filter(clase -> clase.startsWith("ar.uade.cine."))
                .map(clase -> clase.substring("ar.uade.cine.".length()))
                .toList();
    }

    private static List<String> referenciasExternas(Path archivo) {
        return referencias(archivo).stream()
                .filter(clase -> !clase.startsWith("ar.uade.cine."))
                .toList();
    }

    // Los import y también los nombres calificados escritos en el código, que se saltean la lista de imports.
    private static List<String> referencias(Path archivo) {
        return NOMBRE_CALIFICADO.matcher(codigoDe(archivo)).results()
                .map(MatchResult::group)
                .toList();
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
