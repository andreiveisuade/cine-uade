package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.JTable;
import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// La columna Retiro: quien atiende ve en ámbar lo cobrado que falta entregar en la Puerta.
class TablaComprasTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "cobrada sin entregar, COBRADA, , A retirar, aviso",
            "cobrada y entregada, COBRADA, 2026-10-28T20:00:00, Retirado, exito",
            "todavia sin cobrar, PENDIENTE, , Sin cobrar, secundario"})
    void elRetiroDiceYPintaComoEstaLaVenta(String caso, String estado, String retiradaEn, String texto,
                                           String color) {
        Tabla<CompraCandy> tabla = TablaCompras.crear();
        tabla.mostrar(List.of(new CompraCandy(1, 7, estado, "2026-10-28T19:00:00", "2026-10-28T19:00:00",
                retiradaEn, "EFECTIVO", null, List.of(), 5000, 0)));

        JTable jtable = tabla.tabla();
        int retiro = jtable.getColumnModel().getColumnIndex("Retiro");
        assertEquals(texto, jtable.getValueAt(0, retiro));
        assertEquals(color(color), jtable.prepareRenderer(jtable.getCellRenderer(0, retiro), 0, retiro)
                .getForeground());
    }

    private static Color color(String nombre) {
        return switch (nombre) {
            case "aviso" -> Colores.aviso();
            case "exito" -> Colores.exito();
            default -> Colores.secundario();
        };
    }
}
