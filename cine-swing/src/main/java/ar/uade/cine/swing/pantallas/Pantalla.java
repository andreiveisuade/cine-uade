package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.Componentes;

import javax.swing.BorderFactory;
import java.awt.BorderLayout;

// Lo común a toda pantalla del panel: margen y encabezado; los atajos para pedir y avisar vienen de Seccion.
public abstract class Pantalla extends Seccion {

    protected Pantalla(String titulo, String descripcion) {
        super(new BorderLayout(Componentes.BLOQUE, Componentes.APARTE));
        setBorder(BorderFactory.createEmptyBorder(Componentes.BLOQUE, Componentes.MARGEN, Componentes.BLOQUE,
                Componentes.MARGEN));
        add(Componentes.encabezado(titulo, descripcion), BorderLayout.NORTH);
    }
}
