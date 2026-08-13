package ar.uade.cine.dominio.candy;

/**
 * Una línea de la compra: qué producto, cuántos y a cuánto. El precio se congela igual
 * que en las entradas, para que el ticket emitido siga siendo válido aunque después
 * cambie la lista de precios.
 */
public record ItemCompra(int productoId, String nombre, int cantidad, double precioUnitario) {

    public double getSubtotal() {
        return precioUnitario * cantidad;
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
