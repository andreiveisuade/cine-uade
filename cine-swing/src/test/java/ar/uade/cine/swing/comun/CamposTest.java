package ar.uade.cine.swing.comun;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Los filtros de los campos numéricos: lo que no es número ni se llega a tipear.
class CamposTest {

    @Test
    void losFiltrosNoDejanTipearLoQueNoEsNumero() {
        JTextField entero = Campos.soloEntero(new JTextField());
        JTextField decimal = Campos.soloDecimal(new JTextField());
        JTextField lista = Campos.soloListaDeEnteros(new JTextField());

        entero.setText("12a");
        decimal.setText("2500,5");
        lista.setText("8, 10,12");

        assertEquals("", entero.getText());
        assertEquals("2500,5", decimal.getText());
        assertEquals("8, 10,12", lista.getText());

        lista.setText("8,x");
        assertEquals("8, 10,12", lista.getText());
    }
}
