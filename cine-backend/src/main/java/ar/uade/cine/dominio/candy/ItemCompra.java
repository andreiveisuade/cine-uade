package ar.uade.cine.dominio.candy;

import ar.uade.cine.dominio.dinero.Dinero;

/**
 * Una línea de la compra: qué producto, cuántos y a cuánto. El precio se congela igual
 * que en las entradas, para que el ticket emitido siga siendo válido aunque después
 * cambie la lista de precios.
 */
public record ItemCompra(int productoId, String nombre, int cantidad, Dinero precioUnitario) {

    public Dinero getSubtotal() {
        return precioUnitario.por(cantidad);
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}
