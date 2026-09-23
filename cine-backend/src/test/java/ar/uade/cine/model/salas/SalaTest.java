package ar.uade.cine.model.salas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class SalaTest {

    private static void rechaza(String mensaje, Executable accion) {
        assertEquals(mensaje, assertThrows(IllegalArgumentException.class, accion).getMessage());
    }

    @Test
    void unaSalaSinNombreSinTipoOConLimpiezaNegativaNoSeConstruye() {
        rechaza("El nombre no puede estar vacío", () -> new Sala(null, TipoSala.DOS_D, 15));
        rechaza("El nombre no puede estar vacío", () -> new Sala("  ", TipoSala.DOS_D, 15));
        rechaza("Falta el tipo de sala", () -> new Sala("Sala 1", null, 15));
        rechaza("Los minutos de limpieza no pueden ser negativos", () -> new Sala("Sala 1", TipoSala.DOS_D, -1));
    }

    @Test
    void editarPideLoMismoQueElAltaYNoTocaNadaSiRechaza() {
        Sala sala = new Sala("Sala 1", TipoSala.DOS_D, 15);

        rechaza("El nombre no puede estar vacío", () -> sala.editar("", TipoSala.TRES_D, 10));
        rechaza("Falta el tipo de sala", () -> sala.editar("Sala 2", null, 10));
        rechaza("Los minutos de limpieza no pueden ser negativos", () -> sala.editar("Sala 2", TipoSala.TRES_D, -5));

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
