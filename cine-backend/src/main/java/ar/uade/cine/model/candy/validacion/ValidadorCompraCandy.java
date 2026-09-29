package ar.uade.cine.model.candy.validacion;

import java.util.Map;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;
import ar.uade.cine.model.ventas.MedioPago;

// Lo que pide una venta del candy: productos, medio de pago y renglones vendibles; lo llama CompraCandy.
// ItemCompra lo llama por renglón. El código de autorización lo valida MedioPago (R11), que sabe qué medio
// lo lleva.
public final class ValidadorCompraCandy {

    private ValidadorCompraCandy() {
    }

    public static void exigirProductos(Map<Producto, Integer> cantidades) {
        Regla.lista(cantidades == null ? null : cantidades.keySet())
                .noVacia("Hay que elegir al menos un producto");
    }

    public static MedioPago medio(MedioPago medio) {
        return Regla.objeto(medio).obligatorio("Falta el medio de pago").valor();
    }

    // Por renglón: la cantidad es la misma regla que en un combo; lo que se sacó de la carta no se vende.
    public static int renglon(Producto producto, Integer cantidad) {
        int valida = ValidadorCantidad.valida(cantidad, producto.getNombre());
        if (!producto.estaDisponible()) {
            throw new DatoInvalido(producto.getNombre() + " no está disponible");
        }
        return valida;
    }
}
