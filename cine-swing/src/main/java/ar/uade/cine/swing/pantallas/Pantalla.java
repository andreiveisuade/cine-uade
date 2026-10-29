package ar.uade.cine.swing.pantallas;

import ar.uade.cine.swing.comun.Componentes;
import ar.uade.cine.swing.comun.Escala;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

// Lo común a toda pantalla del panel: margen y encabezado; los atajos para pedir y avisar vienen de Seccion.
public abstract class Pantalla extends Seccion {

    private final JPanel cabecera = new JPanel(new BorderLayout(Escala.BLOQUE, 0));

    protected Pantalla(String titulo, String descripcion) {
        super(new BorderLayout(Escala.BLOQUE, Escala.APARTE));
        setBorder(BorderFactory.createEmptyBorder(Escala.BLOQUE, Escala.MARGEN, Escala.BLOQUE,
                Escala.MARGEN));
        cabecera.add(Componentes.encabezado(titulo, descripcion), BorderLayout.CENTER);
        add(cabecera, BorderLayout.NORTH);
    }

    /** Lo que hace la pantalla entera (exportar, traer la cartelera): arriba a la derecha, a la altura del título. */
    protected void accionDeLaPantalla(JComponent... acciones) {
        cabecera.add(Componentes.aLaDerecha(acciones), BorderLayout.EAST);
    }
}
