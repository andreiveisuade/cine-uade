package ar.uade.cine.ui;

import ar.uade.cine.dominio.ventas.MedioPago;

/**
 * El código de autorización, pedido solo cuando el medio de pago lo necesita (R11).
 *
 * <p>Está afuera de los menús porque cobrar una reserva y vender en el candy son dos
 * circuitos distintos que hacen exactamente lo mismo acá: si cada uno tuviera su copia,
 * agregar un medio de pago nuevo obligaría a acordarse de los dos.
 *
 * <p>Quién exige el código no se decide acá: se le pregunta a la constante, que es la que
 * sabe. El gestor lo vuelve a validar igual, porque una interfaz de usuario no es un
 * lugar donde apoyar una regla.
 */
final class PedidoDeAutorizacion {

    private PedidoDeAutorizacion() {
    }

    static String pedir(Consola consola, MedioPago medio) {
        if (!medio.requiereAutorizacion()) {
            return "";
        }
        return consola.leerTexto("Código de autorización: ");
    }
}
