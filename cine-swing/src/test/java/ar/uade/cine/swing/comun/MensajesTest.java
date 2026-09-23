package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sin ventana: si el éxito abriera un diálogo, en headless esto explotaría. */
class MensajesTest {

    @Test
    void elExitoVaALaBarraDeLaVentanaDeLaPantalla() {
        JRootPane raiz = new JRootPane();
        JLabel barra = Mensajes.barraDeEstado(raiz);
        JPanel pantalla = new JPanel();
        raiz.getContentPane().add(pantalla);

        Mensajes.exito(pantalla, "Sala creada");

        assertEquals("Sala creada", barra.getText());
        assertEquals(Colores.exito(), barra.getForeground());
    }
}
