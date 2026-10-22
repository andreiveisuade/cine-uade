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
import static ar.uade.cine.swing.comun.Formato.porcentaje;

// Las películas que eligió el planificador para la semana, con cuántas funciones le tocaron a cada una.
final class ElencoSemana extends JPanel {

    ElencoSemana(List<PeliculaElegida> elenco, int funciones) {
        super(new BorderLayout(0, 6));
        Tabla<PeliculaElegida> tabla = new Tabla<>(
                Columna.<PeliculaElegida>de("Película", p -> p.titulo()).ancho(240),
                Columna.<PeliculaElegida>de("Géneros", p -> p.generos().stream().map(g -> etiqueta(g))
                        .collect(Collectors.joining(" · "))).ancho(220),
                Columna.<PeliculaElegida>numero("Duración", p -> duracion(p.duracionMinutos())),
                Columna.<PeliculaElegida>numero("Puntaje", p -> conDecimal(p.puntaje())),
                Columna.<PeliculaElegida>numero("Funciones", p -> p.pases() + " · "
                        + porcentaje((double) p.pases() / funciones)));
        tabla.mostrar(elenco);
        JScrollPane scroll = tabla.conScroll();
        int alto = Tabla.altoPara(elenco.size());
        scroll.setPreferredSize(new Dimension(600, alto));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        add(Componentes.subtitulo("Elenco (" + elenco.size() + ")"), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(Componentes.nota("La primera entra por puntaje; las demás, por puntaje y los géneros que suman."),
                BorderLayout.SOUTH);
    }
}
