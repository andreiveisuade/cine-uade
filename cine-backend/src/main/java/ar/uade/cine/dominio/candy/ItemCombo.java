package ar.uade.cine.dominio.candy;

/**
 * Un producto que viene adentro de un combo, con cuántas unidades trae. Es lo que
 * convierte al combo en una promoción de verdad y no en un producto con nombre bonito:
 * sabiendo qué contiene se puede comparar su precio contra el de comprarlo suelto.
 *
 * @param nombre copiado del producto para armar el detalle del ticket sin otra consulta
 */
public record ItemCombo(int productoId, String nombre, int cantidad) {

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
