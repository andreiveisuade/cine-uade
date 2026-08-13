package ar.uade.cine.ui;

/**
 * Una entrada del menú principal.
 *
 * <p>Existe para que el menú principal no tenga que conocer a cada uno: recibe la lista,
 * la numera y delega en el que corresponda. Sumar un área del negocio a la consola es
 * escribir su menú y agregarlo a esa lista, sin tocar un switch que hay que ir a buscar
 * —y sin que el compilador te avise si te olvidaste—.
 */
public interface Menu {

    /** Lo que se lee en el menú principal. */
    String titulo();

    /** Muestra las opciones del área y atiende la que se elija. */
    void mostrar();
}
