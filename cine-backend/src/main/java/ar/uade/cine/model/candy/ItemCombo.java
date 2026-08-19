package ar.uade.cine.model.candy;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Un producto que viene adentro de un combo, con cuántas unidades trae. Es lo que convierte
 * al combo en una promoción de verdad y no en un producto con nombre bonito: sabiendo qué
 * contiene se puede comparar su precio contra el de comprarlo suelto.
 *
 * <p>Es {@code @Embeddable} y no una entidad porque no tiene identidad propia: la fila de
 * {@code combo_item} se identifica por el combo y el producto, que es exactamente su clave
 * primaria. Vive y muere con el combo que la contiene.
 *
 * <p>Al producto lo referencia por objeto y no por id porque el detalle del ticket necesita
 * su nombre, y ese dato vive del otro lado. Antes lo traía un JOIN escrito a mano.
 */
@Embeddable
public class ItemCombo {

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    private int cantidad;

    protected ItemCombo() {
    }

    public ItemCombo(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public int productoId() {
        return producto.getId();
    }

    /** Copiado del producto para armar el detalle del ticket sin otra consulta. */
    public String nombre() {
        return producto.getNombre();
    }

    public int cantidad() {
        return cantidad;
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre();
    }
}
