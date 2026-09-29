package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.dto.informes.InformeFuncion;
import ar.uade.cine.swing.comun.Componentes;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;

import static ar.uade.cine.swing.comun.Formato.precio;

// Cuánto dejó una función entre las dos cajas del cine, boletería y candy; el de mostrador no entra.
final class PanelInforme extends JPanel {

    PanelInforme(InformeFuncion informe) {
        super(new BorderLayout());
        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.add(Componentes.izquierda(Componentes.subtitulo("Informe de la función")));
        contenido.add(Componentes.izquierda(Componentes.nota("Cuánto dejó la función entre las dos cajas del cine: "
                + "boletería y candy.")));
        contenido.add(Box.createVerticalStrut(8));
        contenido.add(Componentes.izquierda(Componentes.renglon("Boletería",
                precio(informe.boleteria().recaudacionNeta()), false)));
        contenido.add(Componentes.izquierda(Componentes.nota(informe.boleteria().espectadores()
                + " entradas cobradas, ya con los descuentos")));
        contenido.add(Componentes.izquierda(Componentes.renglon("Candy", precio(informe.candy()), false)));
        contenido.add(Componentes.izquierda(Componentes.nota(informe.comprasCandy() + (informe.comprasCandy() == 1
                ? " compra atribuida" : " compras atribuidas") + " a esta función")));
        contenido.add(Componentes.izquierda(new JSeparator()));
        contenido.add(Componentes.izquierda(Componentes.renglon("Total", precio(informe.total()), true)));
        contenido.add(Box.createVerticalStrut(12));
        contenido.add(Componentes.izquierda(Componentes.nota("<b>El candy de mostrador no entra, a propósito.</b> "
                + "Solo se le atribuye a una función lo que se compró junto con la entrada, porque esa reserva es lo "
                + "único que dice de qué función se trata. Por eso la suma de los informes de todas las funciones de "
                + "un día da menos que el arqueo de ese día, y la diferencia es el mostrador.")));
        contenido.add(Box.createVerticalGlue());
        add(Componentes.conBorde(contenido));
    }
}
