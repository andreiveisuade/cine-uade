package ar.uade.cine.model.salas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
