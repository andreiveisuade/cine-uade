package ar.uade.cine.swing.comun;

import ar.uade.cine.swing.comun.Tabla.Columna;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.JTable;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Un estado de dos caras: el texto y el color de la pastilla salen del mismo dato de la fila.
class TablaTest {

    private record Fila(boolean activa) {
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "vigente en verde, true, Activa",
            "apagada en gris, false, Dada de baja"})
    void elEstadoDiceYPintaSegunLaFila(String caso, boolean activa, String texto) {
        Tabla<Fila> tabla = new Tabla<>(Columna.estado("Estado", Fila::activa, "Activa", "Dada de baja"));
        tabla.mostrar(List.of(new Fila(activa)));

        JTable jtable = tabla.tabla();
        Component celda = jtable.prepareRenderer(jtable.getCellRenderer(0, 0), 0, 0);
        assertEquals(texto, jtable.getValueAt(0, 0));
        assertEquals(activa ? Colores.exito() : Colores.secundario(), celda.getForeground());
    }
}
