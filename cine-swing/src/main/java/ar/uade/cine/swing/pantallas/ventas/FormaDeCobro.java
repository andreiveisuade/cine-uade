package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;

// Cómo se cobra una reserva con el medio elegido: qué dice el botón, qué se explica y qué se le pide al backend.
// Patrón Strategy: el texto, la confirmación y el pedido dependían del mismo if repetido en PanelCobro; ahora cada
// forma de cobro es una clase y PanelCobro (el Context) solo elige una y le delega.
interface FormaDeCobro {

    String boton();

    String explicacion();

    void enviar(Reserva reserva, MedioPago medio, CajaDeCobro caja);

    // R11: en un medio electrónico la autorización la devuelve el procesador, no se tipea. Lo decide el catálogo,
    // nunca el nombre del medio.
    static FormaDeCobro para(MedioPago medio, ApiVentas api) {
        return medio != null && medio.requiereAutorizacion() ? new CobroPorCheckout(api) : new CobroEnCaja(api);
    }
}
