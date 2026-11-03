package ar.uade.cine.swing.comun;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListadoTest {

    // Con la ventana angosta (1280 con el formulario al costado) los botones se encimaban con el conteo.
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "ancha: conteo y botones en una fila, 1200, West, East",
            "angosta: los botones bajan debajo del conteo, 200, North, South",
    })
    void losBotonesBajanSiNoEntranAlLadoDelConteo(String caso, int ancho, String resumen, String acciones) {
        JLabel conteo = new JLabel("20 cargadas · 2 publicadas");
        JPanel botones = Componentes.aLaDerecha(new JButton("Editar"), new JButton("Publicar / despublicar"));
        JPanel listado = Componentes.listado(null, conteo, botones, new JTable());
        Container barra = conteo.getParent();

        barra.setSize(ancho, 40);
        barra.getComponentListeners()[0].componentResized(null);

        BorderLayout layout = (BorderLayout) barra.getLayout();
        assertEquals(resumen, layout.getConstraints(conteo));
        assertEquals(acciones, layout.getConstraints(botones));
    }
}
