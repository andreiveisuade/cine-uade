package ar.uade.cine.dominio.candy;

import java.util.List;
import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Algo que se vende en el candy. Un combo es también un Producto —tiene precio y se
 * vende como una unidad— y no una entidad aparte: lo único que lo distingue es que
 * además sabe qué trae adentro.
 */
public interface Producto {

    int getId();

    void setId(int id);

    String getNombre();

    TipoProducto getTipo();

    Dinero getPrecio();

    void setPrecio(Dinero precio);

    /**
     * Si se sigue ofreciendo. Un producto no se borra: puede estar en compras viejas,
     * y borrarlo dejaría esos tickets apuntando a la nada.
     */
    boolean estaDisponible();

    void setDisponible(boolean disponible);

    /** Qué trae el combo. Lista vacía en un producto suelto. */
    List<ItemCombo> getComponentes();

    void agregarComponente(ItemCombo componente);

    boolean esCombo();
}
