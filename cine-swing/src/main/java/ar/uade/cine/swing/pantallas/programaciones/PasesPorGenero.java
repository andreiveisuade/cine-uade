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
import static ar.uade.cine.swing.comun.Formato.porcentaje;

// Una barra por género con qué parte de las funciones de la semana lo tiene, de mayor a menor.
final class PasesPorGenero extends JPanel {

    // La barra es contra el total y no contra el mayor: una al tope dice «todas las funciones», no «la que más».
    PasesPorGenero(Map<String, Integer> pasesPorGenero, int funciones) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (pasesPorGenero == null || pasesPorGenero.isEmpty()) return;
        add(Componentes.izquierda(Componentes.subtitulo("Funciones por género")));
        List<Map.Entry<String, Integer>> entradas = new ArrayList<>(pasesPorGenero.entrySet());
        entradas.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        entradas.forEach(e -> add(Componentes.izquierda(barra(etiqueta(e.getKey()), e.getValue(), funciones))));
        add(Componentes.izquierda(Componentes.nota("Una película suma en cada uno de sus géneros.")));
    }

    private static JPanel barra(String genero, int pases, int funciones) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        JLabel nombre = new JLabel(genero);
        nombre.setPreferredSize(new Dimension(130, nombre.getPreferredSize().height));
        JProgressBar barra = new JProgressBar(0, funciones);
        barra.setValue(pases);
        barra.setStringPainted(true);
        barra.setString(pases + " · " + porcentaje((double) pases / funciones));
        fila.add(nombre, BorderLayout.WEST);
        fila.add(barra, BorderLayout.CENTER);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        fila.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return fila;
    }
}
