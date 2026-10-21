package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;

// Cómo se cobra una reserva con el medio elegido: qué dice el botón, qué se explica y qué se le pide al backend.
// Patrón Strategy: cada forma de cobro es una clase; PanelCobro (Context) elige una y delega.
interface FormaDeCobro {

    String boton();

    String explicacion();

    void enviar(Reserva reserva, MedioPago medio, CajaDeCobro caja);

    // R11: la autorización la devuelve el procesador en un medio electrónico; lo dice el catálogo, no el nombre.
    static FormaDeCobro para(MedioPago medio, ApiVentas api) {
        return medio != null && medio.requiereAutorizacion() ? new CobroPorCheckout(api) : new CobroEnCaja(api);
    }
}
