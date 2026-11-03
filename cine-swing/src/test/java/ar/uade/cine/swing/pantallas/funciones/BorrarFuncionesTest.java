package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.dto.funciones.Borrado;
import ar.uade.cine.swing.api.dto.funciones.FuncionRechazada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Lo que dice la barra después de un borrado en lote, y el porqué de las rechazadas con el texto del backend.
class BorrarFuncionesTest {

    private static Borrado borrado(int borradas, int rechazadas) {
        return new Borrado(IntStream.rangeClosed(1, borradas).boxed().toList(),
                IntStream.rangeClosed(1, rechazadas)
                        .mapToObj(id -> new FuncionRechazada(100 + id, "La función " + (100 + id) + " tiene reservas"))
                        .toList());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            una             | 1  | 0 | Función borrada
            varias          | 75 | 0 | Se borraron 75 funciones
            con rechazadas  | 75 | 5 | Se borraron 75 funciones; 5 no se pudieron borrar y quedaron seleccionadas
            una y una       | 1  | 1 | Se borró 1 función; 1 no se pudo borrar y quedó seleccionada
            ninguna borrada | 0  | 3 |
            """)
    void elResumenCuentaLasBorradasYLasQueQuedaron(String caso, int borradas, int rechazadas, String aviso) {
        assertEquals(aviso, BorrarFunciones.resumen(borrado(borradas, rechazadas)).orElse(null));
    }

    @Test
    void conMuchasRechazadasSeLeenLasPrimerasYElRestoSeCuenta() {
        String errores = BorrarFunciones.errores(borrado(0, 12)).orElseThrow();

        List<String> lineas = List.of(errores.split("\n"));
        assertEquals(11, lineas.size());
        assertEquals("La función 101 tiene reservas", lineas.get(0));
        assertEquals("y 2 más", lineas.get(10));
    }
}
