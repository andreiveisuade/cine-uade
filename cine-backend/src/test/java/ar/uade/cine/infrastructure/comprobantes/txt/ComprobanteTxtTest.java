package ar.uade.cine.infrastructure.comprobantes.txt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ar.uade.cine.infrastructure.comprobantes.ComprobanteException;

// La plantilla con un comprobante mínimo: el orden de los pasos lo fija la base, no quien la hereda.
class ComprobanteTxtTest {

    private static final String LINEA = "=".repeat(44);

    @TempDir
    Path carpeta;

    @Test
    void imprimeEncabezadoCuerpoYLeyendasEntreLineas() throws IOException {
        new Prueba(carpeta).imprimir("hola");

        assertEquals(List.of(
                LINEA, "                 CINE UADE", "                PRUEBA hola", LINEA,
                " Dato         : hola",
                LINEA, "                  Leyenda", LINEA),
                Files.readAllLines(carpeta.resolve("prueba-hola.txt")));
    }

    @Test
    void siNoPuedeEscribirDiceQueComprobanteFallo() throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("ocupado"), "no es una carpeta");

        ComprobanteException e = assertThrows(ComprobanteException.class, () -> new Prueba(archivo).imprimir("x"));

        assertEquals("No se pudo emitir la prueba x", e.getMessage());
    }

    private static class Prueba extends ComprobanteTxt<String> {

        Prueba(Path directorio) {
            super(directorio);
        }

        @Override
        protected String titulo(String dato) {
            return "PRUEBA " + dato;
        }

        @Override
        protected List<String> cuerpo(String dato) {
            return List.of(campo("Dato", dato));
        }

        @Override
        protected List<String> leyendas() {
            return List.of("Leyenda");
        }

        @Override
        protected String archivo(String dato) {
            return "prueba-" + dato + ".txt";
        }

        @Override
        protected String queEs(String dato) {
            return "la prueba " + dato;
        }
    }
}
