package ar.uade.cine.model.candy.validacion;

import ar.uade.cine.model.candy.Producto;
import ar.uade.cine.model.candy.TipoProducto;
import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.rechazos.DatoInvalido;
import ar.uade.cine.model.validacion.Regla;

// Los datos de un producto de la carta (nombre, tipo, precio) y R14 al editarlo; lo llama solo Producto.
// Producto se queda con su estado y sus transiciones; acá, qué dato no puede entrar. Cada guarda devuelve
// el valor ya limpio, así Producto valida todo antes de asignar nada.
public final class ValidadorProducto {

    // El VARCHAR(60) de la tabla: pasado, MySQL rechaza el INSERT y el usuario vería un 500.
    private static final int LARGO_MAXIMO_DEL_NOMBRE = 60;

    private ValidadorProducto() {
    }

    // Recortado acá: el nombre repetido se busca con el mismo valor que se guarda.
    public static String nombre(String nombre) {
        return Regla.texto(nombre).obligatorio("Falta el nombre").recortado()
                .hasta(LARGO_MAXIMO_DEL_NOMBRE, "El nombre").valor();
    }

    // Va antes que el nombre: un combo sin nombre se rechaza por ser combo, que es el error de fondo.
    public static void exigirQueNoSeaCombo(TipoProducto tipo) {
        if (tipo != null && tipo.esCombo()) {
            throw new DatoInvalido("Un combo se da de alta como combo: con sus componentes");
        }
    }

    public static TipoProducto tipo(TipoProducto tipo) {
        return Regla.objeto(tipo).obligatorio("Falta el tipo de producto").valor();
    }

    public static Dinero precio(Dinero precio) {
        return Dinero.importeValido(precio, "precio");
    }

    // R14 del lado de la edición: un combo que pasa a costar lo mismo que sus sueltos ya no conviene. Lo usa
    // también el combo que trae un suelto recién abaratado, con su propio nombre y precio.
    public static void exigirQueSigaConviniendo(Producto producto, String nombre, Dinero precio) {
        if (producto.esCombo() && !producto.getPrecioSuelto().esMayorQue(precio)) {
            throw new DatoInvalido("Con ese precio, el combo " + nombre + " dejaría de salir menos que sus"
                    + " componentes sueltos ($ " + producto.getPrecioSuelto() + ")");
        }
    }
}
