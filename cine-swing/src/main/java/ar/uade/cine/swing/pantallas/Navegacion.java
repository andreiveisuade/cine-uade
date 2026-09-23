package ar.uade.cine.swing.pantallas;

import javax.swing.JComponent;

public interface Navegacion {

    /** Va a una entrada del menú, y la marca. */
    void ir(Destino destino);

    /** Muestra una pantalla de detalle que no está en el menú, como el borderó de una función. */
    void abrir(JComponent pantalla);
}
