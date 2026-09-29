package ar.uade.cine.model.salas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.uade.cine.model.rechazos.Rechazo;

class SalaTest {

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(Rechazo.class, accion).getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            sin nombre,          ,       DOS_D, 15,  Falta el nombre
            nombre en blanco,    '  ',   DOS_D, 15,  Falta el nombre
            nombre de 51,        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, DOS_D, 15, El nombre no puede tener más de 50 caracteres
            sin tipo,            Sala 1, ,      15,  Falta el tipo de sala
            limpieza negativa,   Sala 1, DOS_D, -1,  Los minutos de limpieza no pueden ser negativos
            # Sin tope, 2147483647 desbordaba el margen con que R3 busca los choques.
            limpieza de 121,     Sala 1, DOS_D, 121, La limpieza no puede durar más de 120 minutos
            limpieza enorme,     Sala 1, DOS_D, 2147483647, La limpieza no puede durar más de 120 minutos
            """)
    void unaSalaSinNombreSinTipoOConLimpiezaNegativaNoSeConstruye(String caso, String nombre, TipoSala tipo,
            int limpieza, String mensaje) {
        rechaza(mensaje, () -> new Sala(nombre, tipo, limpieza));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(textBlock = """
            nombre vacío,      '',     TRES_D, 10,  Falta el nombre
            sin tipo,          Sala 2, ,       10,  Falta el tipo de sala
            limpieza negativa, Sala 2, TRES_D, -5,  Los minutos de limpieza no pueden ser negativos
            limpieza de 121,   Sala 2, TRES_D, 121, La limpieza no puede durar más de 120 minutos
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

        sala.editar("Sala VIP", TipoSala.TRES_D, 120);

        assertEquals("Sala VIP", sala.getNombre());
        assertEquals(TipoSala.TRES_D, sala.getTipo());
        assertEquals(120, sala.getMinutosLimpieza());
    }

    // strip y no trim: trim saca solo los espacios ASCII, y el de un copiar y pegar (U+2003) quedaba.
    @Test
    void elNombreSeGuardaSinEspaciosDeMasEnElAltaYEnLaEdicionYSeMideYaRecortado() {
        Sala sala = new Sala("\u2003Sala 1 ", TipoSala.DOS_D, 15);
        assertEquals("Sala 1", sala.getNombre());

        sala.editar(" " + "x".repeat(50) + " ", TipoSala.DOS_D, 15);
        assertEquals("x".repeat(50), sala.getNombre());
    }

    @Test
    void lasEspecialesSeReconocenComoLasTipeaElEncargadoYLasVaciasSeSaltean() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);
        Map<TipoAsiento, List<String>> especiales = new EnumMap<>(TipoAsiento.class);
        especiales.put(TipoAsiento.VIP, List.of(" a2 "));
        especiales.put(TipoAsiento.PAREJA, List.of("  "));
        especiales.put(TipoAsiento.ACCESIBLE, Arrays.asList((String) null));

        List<Asiento> asientos = sala.generarAsientos(List.of(2), especiales);

        assertEquals(List.of(TipoAsiento.ESTANDAR, TipoAsiento.VIP), asientos.stream().map(Asiento::getTipo).toList());
    }

    // Antes ganaba la última lista en silencio. Repetida en la misma lista no es un problema: el tipo es uno.
    @Test
    void unaButacaEnDosListasSeRechazaYRepetidaEnLaMismaNo() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("La butaca A1 está en más de una lista de especiales: dejala en una sola",
                () -> sala.generarAsientos(List.of(2), Map.of(TipoAsiento.VIP, List.of("A1"),
                        TipoAsiento.PAREJA, List.of(" a1"))));
        assertEquals(TipoAsiento.VIP, sala.generarAsientos(List.of(2),
                Map.of(TipoAsiento.VIP, List.of("A1", "a1"))).get(0).getTipo());
    }

    @Test
    void unaDistribucionSinFilasConDemasiadasOConUnaFilaVaciaNoGeneraButacas() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("La sala tiene que tener al menos una fila", () -> sala.generarAsientos(null, Map.of()));
        rechaza("La sala tiene que tener al menos una fila", () -> sala.generarAsientos(List.of(), Map.of()));
        rechaza("La sala tiene que tener como máximo 26 filas: se identifican con una letra",
                () -> sala.generarAsientos(Collections.nCopies(27, 1), Map.of()));
        rechaza("Cada fila tiene que tener al menos una butaca", () -> sala.generarAsientos(List.of(3, 0), Map.of()));
        rechaza("Cada fila tiene que tener al menos una butaca",
                () -> sala.generarAsientos(Arrays.asList(3, null), Map.of()));
        rechaza("Cada fila tiene que tener al menos una butaca",
                () -> sala.generarAsientos(List.of(41, 0), Map.of()));
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
                Map.of(TipoAsiento.VIP, List.of("B3", " z99"))));
    }

    @Test
    void generaSusButacasFilaPorFilaConLasEspecialesMarcadas() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        List<Asiento> asientos = sala.generarAsientos(List.of(2, 3), Map.of(TipoAsiento.VIP, List.of("B3")));

        assertEquals(List.of("A1", "A2", "B1", "B2", "B3"), asientos.stream().map(Asiento::getCodigo).toList());
        assertEquals(List.of(TipoAsiento.ESTANDAR, TipoAsiento.ESTANDAR, TipoAsiento.ESTANDAR,
                TipoAsiento.ESTANDAR, TipoAsiento.VIP), asientos.stream().map(Asiento::getTipo).toList());
        asientos.forEach(asiento -> assertSame(sala, asiento.getSala()));
        assertEquals("Z1", sala.generarAsientos(Collections.nCopies(26, 1), Map.of()).get(25).getCodigo());
    }
}
