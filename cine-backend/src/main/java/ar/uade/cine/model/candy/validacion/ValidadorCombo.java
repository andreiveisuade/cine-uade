package ar.uade.cine.model.candy.validacion;

import java.util.Map;

import ar.uade.cine.model.candy.Combo;
import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;

// Lo que pide un combo al armarse: dos componentes o más, ninguno combo, y R14; lo llama Combo.
public final class ValidadorCombo {

    private ValidadorCombo() {
    }

    // Un combo de uno es un producto suelto con otro precio.
    public static void exigirComponentes(Map<Producto, Integer> componentes) {
        if (componentes == null || componentes.size() < 2) {
            throw new DatoInvalido("Un combo tiene que juntar al menos dos productos distintos");
        }
    }

    // Por componente: primero la cantidad, después si es combo (un combo anidado haría recursivo el ahorro).
    public static int componente(Producto producto, Integer cantidad) {
        int valida = ValidadorCantidad.valida(cantidad, producto.getNombre());
        if (producto.esCombo()) {
            throw new DatoInvalido("Un combo no puede contener otro combo: " + producto.getNombre());
        }
        return valida;
    }

    // R14: un combo que no sale menos que sus componentes sueltos no tiene por qué comprarse.
    public static void exigirQueConvenga(Combo combo, Dinero precio) {
        if (!combo.getPrecioSuelto().esMayorQue(precio)) {
            throw new DatoInvalido("El combo tiene que salir menos que sus componentes sueltos ($ "
                    + combo.getPrecioSuelto() + ")");
        }
    }

    // R14 al editar; lo usa también el combo que trae un suelto recién abaratado.
    public static void exigirQueSigaConviniendo(Combo combo, String nombre, Dinero precio) {
        if (!combo.getPrecioSuelto().esMayorQue(precio)) {
            throw new DatoInvalido("Con ese precio, el combo " + nombre + " dejaría de salir menos que sus"
                    + " componentes sueltos ($ " + combo.getPrecioSuelto() + ")");
        }
    }
}
