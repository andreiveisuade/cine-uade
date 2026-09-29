package ar.uade.cine.swing.pantallas.informes;

import ar.uade.cine.swing.api.dto.informes.InformeFuncion;
import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Pila;

import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;

import static ar.uade.cine.swing.comun.Formato.cantidad;
import static ar.uade.cine.swing.comun.Formato.precio;

// Cuánto dejó una función entre las dos cajas del cine, boletería y candy; el de mostrador no entra.
final class PanelInforme extends JPanel {

    PanelInforme(InformeFuncion informe) {
        super(new BorderLayout());
        Pila contenido = new Pila();
        contenido.agregar(Componentes.subtitulo("Informe de la función"));
        contenido.agregar(Componentes.nota("Cuánto dejó la función entre las dos cajas del cine: "
                + "boletería y candy."));
        contenido.espacio(8);
        contenido.agregar(Componentes.renglon("Boletería",
                precio(informe.boleteria().recaudacionNeta()), false));
        contenido.agregar(Componentes.nota(informe.boleteria().espectadores()
                + " entradas cobradas, ya con los descuentos"));
        contenido.agregar(Componentes.renglon("Candy", precio(informe.candy()), false));
        contenido.agregar(Componentes.nota(cantidad(informe.comprasCandy(), "compra atribuida",
                "compras atribuidas") + " a esta función"));
        contenido.agregar(new JSeparator());
        contenido.agregar(Componentes.renglon("Total", precio(informe.total()), true));
        contenido.espacio(12);
        contenido.agregar(Componentes.nota("<b>El candy de mostrador no entra, a propósito.</b> "
                + "Solo se le atribuye a una función lo que se compró junto con la entrada, porque esa reserva es lo "
                + "único que dice de qué función se trata. Por eso la suma de los informes de todas las funciones de "
                + "un día da menos que el arqueo de ese día, y la diferencia es el mostrador."));
        contenido.relleno();
        add(Componentes.conBorde(contenido));
    }
}
