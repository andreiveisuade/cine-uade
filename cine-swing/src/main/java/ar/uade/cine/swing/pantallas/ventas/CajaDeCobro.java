package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.dto.ventas.Checkout;
import ar.uade.cine.swing.api.dto.ventas.Pago;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

// Lo que una FormaDeCobro necesita del panel: preguntar, pedir fuera del EDT y mostrar cómo terminó.
// Interfaz de rol (ISP): la estrategia no ve el resto de PanelCobro, y se prueba sin abrir ventanas.
interface CajaDeCobro {

    boolean confirmar(String pregunta, String si);

    <T> void pedir(Callable<T> trabajo, Consumer<T> alTerminar);

    void cobrado(Pago pago);

    void mostrarCheckout(Checkout checkout);
}
