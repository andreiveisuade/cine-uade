package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.comun.Componentes;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Una barra por género con cuántos pases tiene en la semana, de mayor a menor; sin géneros queda vacío.
final class PasesPorGenero extends JPanel {

    PasesPorGenero(Map<String, Integer> pasesPorGenero) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (pasesPorGenero == null || pasesPorGenero.isEmpty()) return;
        add(Componentes.izquierda(Componentes.subtitulo("Pases por género")));
        List<Map.Entry<String, Integer>> entradas = new ArrayList<>(pasesPorGenero.entrySet());
        entradas.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        int maximo = entradas.get(0).getValue();
        entradas.forEach(e -> add(Componentes.izquierda(barra(etiqueta(e.getKey()), e.getValue(), maximo))));
        add(Componentes.izquierda(Componentes.nota("Una película cuenta en todos sus géneros, así que la suma "
                + "es mayor que la cantidad de pases.")));
    }

    private static JPanel barra(String genero, int pases, int maximo) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        JLabel nombre = new JLabel(genero);
        nombre.setPreferredSize(new Dimension(130, nombre.getPreferredSize().height));
        JProgressBar barra = new JProgressBar(0, maximo);
        barra.setValue(pases);
        barra.setStringPainted(true);
        barra.setString(String.valueOf(pases));
        fila.add(nombre, BorderLayout.WEST);
        fila.add(barra, BorderLayout.CENTER);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        fila.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return fila;
    }
}
