package ar.uade.cine.model.candy.validacion;

import java.util.Map;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;

// Lo que pide un combo al armarse: dos componentes o más, ninguno combo, y R14; lo llama Producto.
// ItemCombo lo llama por cada componente. Nombre y precio son los de cualquier producto: ValidadorProducto.
public final class ValidadorCombo {

    private ValidadorCombo() {
    }

    // Un combo de uno es un producto suelto con otro precio.
    public static void exigirComponentes(Map<Producto, Integer> componentes) {
        if (componentes == null || componentes.size() < 2) {
            throw new DatoInvalido("Un combo tiene que juntar al menos dos productos distintos");
        }
    }

    // Por componente y en este orden, como llega el pedido: primero su cantidad y después si es otro combo.
    // Un combo dentro de otro haría recursivo el precio suelto y el ahorro.
    public static int componente(Producto producto, Integer cantidad) {
        int valida = ValidadorCantidad.valida(cantidad, producto.getNombre());
        if (producto.esCombo()) {
            throw new DatoInvalido("Un combo no puede contener otro combo: " + producto.getNombre());
        }
        return valida;
    }

    // R14: un combo que no sale menos que sus componentes sueltos no tiene por qué comprarse.
    public static void exigirQueConvenga(Producto combo, Dinero precio) {
        if (!combo.getPrecioSuelto().esMayorQue(precio)) {
            throw new DatoInvalido("El combo tiene que salir menos que sus componentes sueltos ($ "
                    + combo.getPrecioSuelto() + ")");
        }
    }
}
