package ar.uade.cine.swing.pantallas.programaciones;

import ar.uade.cine.swing.api.dto.programaciones.PeliculaElegida;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Tabla.Columna;
import ar.uade.cine.swing.comun.Tabla;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.stream.Collectors;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;
import static ar.uade.cine.swing.comun.Formato.conDecimal;
import static ar.uade.cine.swing.comun.Formato.duracion;

// Las películas que eligió el planificador para la semana, con cuántos pases le tocaron a cada una.
final class ElencoSemana extends JPanel {

    ElencoSemana(List<PeliculaElegida> elenco) {
        super(new BorderLayout(0, 6));
        Tabla<PeliculaElegida> tabla = new Tabla<>(
                Columna.<PeliculaElegida>de("Película", p -> p.titulo()).ancho(240),
                Columna.<PeliculaElegida>numero("Puntaje", p -> conDecimal(p.puntaje())),
                Columna.<PeliculaElegida>de("Duración", p -> duracion(p.duracionMinutos())),
                Columna.<PeliculaElegida>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                        .collect(Collectors.joining(" · "))).ancho(220),
                Columna.<PeliculaElegida>numero("Pases", PeliculaElegida::pases));
        tabla.mostrar(elenco);
        JScrollPane scroll = tabla.conScroll();
        int alto = Tabla.altoPara(elenco.size());
        scroll.setPreferredSize(new Dimension(600, alto));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        add(Componentes.subtitulo("Elenco de la semana (" + elenco.size() + ")"), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(Componentes.nota("La primera entró por puntaje; las siguientes, por lo que <b>agregan</b> a lo ya "
                + "elegido: por eso puede entrar una comedia de 7,0 antes que la cuarta de acción de 8,5. Los pases se "
                + "reparten proporcionalmente al puntaje."), BorderLayout.SOUTH);
    }
}
