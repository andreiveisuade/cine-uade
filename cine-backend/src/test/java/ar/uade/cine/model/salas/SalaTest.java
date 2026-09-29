package ar.uade.cine.model.salas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SalaTest {

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin nombre,        ,       DOS_D, 15, El nombre no puede estar vacío
            nombre en blanco,  '  ',   DOS_D, 15, El nombre no puede estar vacío
            sin tipo,          Sala 1, ,      15, Falta el tipo de sala
            limpieza negativa, Sala 1, DOS_D, -1, Los minutos de limpieza no pueden ser negativos
            """)
    void unaSalaSinNombreSinTipoOConLimpiezaNegativaNoSeConstruye(String caso, String nombre, TipoSala tipo,
            int limpieza, String mensaje) {
        rechaza(mensaje, () -> new Sala(nombre, tipo, limpieza));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            nombre vacío,      '',     TRES_D, 10, El nombre no puede estar vacío
            sin tipo,          Sala 2, ,       10, Falta el tipo de sala
            limpieza negativa, Sala 2, TRES_D, -5, Los minutos de limpieza no pueden ser negativos
            """)
    void editarPideLoMismoQueElAltaYNoTocaNadaSiRechaza(String caso, String nombre, TipoSala tipo,
            int limpieza, String mensaje) {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza(mensaje, () -> sala.editar(nombre, tipo, limpieza));

        assertEquals("Sala 1", sala.getNombre());
        assertEquals(TipoSala.DOS_D, sala.getTipo());
        assertEquals(15, sala.getMinutosLimpieza());
    }

    @Test
    void sinLimpiezaEsValidoYEditarCambiaLosTresDatos() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 0);

        sala.editar("Sala VIP", TipoSala.TRES_D, 20);

        assertEquals("Sala VIP", sala.getNombre());
        assertEquals(TipoSala.TRES_D, sala.getTipo());
        assertEquals(20, sala.getMinutosLimpieza());
    }

    @Test
    void elNombreSeGuardaSinEspaciosDeMasEnElAltaYEnLaEdicionYSeMideYaRecortado() {
        Sala sala = new Sala("  Sala 1 ", TipoSala.DOS_D, 15);
        assertEquals("Sala 1", sala.getNombre());

        sala.editar(" " + "x".repeat(50) + " ", TipoSala.DOS_D, 15);
        assertEquals("x".repeat(50), sala.getNombre());
    }

    @Test
    void lasEspecialesSeReconocenComoLasTipeaElEncargadoYLasVaciasSeSaltean() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);
        Map<String, TipoAsiento> especiales = new HashMap<>();
        especiales.put(" a2 ", TipoAsiento.VIP);
        especiales.put("  ", TipoAsiento.PAREJA);
        especiales.put(null, TipoAsiento.ACCESIBLE);

        List<Asiento> asientos = sala.generarAsientos(List.of(2), especiales);

        assertEquals(List.of(TipoAsiento.ESTANDAR, TipoAsiento.VIP), asientos.stream().map(Asiento::getTipo).toList());
    }

    @Test
    void unaDistribucionSinFilasConDemasiadasOConUnaFilaVaciaNoGeneraButacas() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("La sala necesita al menos una fila", () -> sala.generarAsientos(null, Map.of()));
        rechaza("La sala necesita al menos una fila", () -> sala.generarAsientos(List.of(), Map.of()));
        rechaza("Máximo 26 filas: se identifican con una letra",
                () -> sala.generarAsientos(Collections.nCopies(27, 1), Map.of()));
        rechaza("Cada fila debe tener al menos una butaca", () -> sala.generarAsientos(List.of(3, 0), Map.of()));
        rechaza("Cada fila debe tener al menos una butaca",
                () -> sala.generarAsientos(Arrays.asList(3, null), Map.of()));
    }

    // Sin tope, [100000] creaba cien mil butacas.
    @Test
    void unaFilaTieneComoMaximoCuarentaButacas() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("Una fila tiene que tener como máximo 40 butacas",
                () -> sala.generarAsientos(List.of(3, 41), Map.of()));
        assertEquals(40, sala.generarAsientos(List.of(40), Map.of()).size());
    }

    // Antes se ignoraba: la sala quedaba sin la butaca especial que pidió el encargado, sin aviso.
    @Test
    void unaEspecialQueNoCaeEnLaDistribucionSeRechaza() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("La butaca Z99 no existe en la sala", () -> sala.generarAsientos(List.of(2, 3),
                Map.of("B3", TipoAsiento.VIP, " z99", TipoAsiento.VIP)));
    }

    @Test
    void generaSusButacasFilaPorFilaConLasEspecialesMarcadas() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        List<Asiento> asientos = sala.generarAsientos(List.of(2, 3), Map.of("B3", TipoAsiento.VIP));

        assertEquals(List.of("A1", "A2", "B1", "B2", "B3"), asientos.stream().map(Asiento::getCodigo).toList());
        assertEquals(List.of(TipoAsiento.ESTANDAR, TipoAsiento.ESTANDAR, TipoAsiento.ESTANDAR,
                TipoAsiento.ESTANDAR, TipoAsiento.VIP), asientos.stream().map(Asiento::getTipo).toList());
        asientos.forEach(asiento -> assertSame(sala, asiento.getSala()));
        assertEquals("Z1", sala.generarAsientos(Collections.nCopies(26, 1), Map.of()).get(25).getCodigo());
    }
}
