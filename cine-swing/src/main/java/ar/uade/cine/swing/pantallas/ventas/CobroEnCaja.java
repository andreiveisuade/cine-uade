package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;

import static ar.uade.cine.swing.comun.Etiquetas.etiqueta;

// Cobro en la caja del cine: se registra el pago en el momento, sin código de autorización.
// Patrón Strategy: ConcreteStrategy de FormaDeCobro para los medios que no requieren autorización.
record CobroEnCaja(ApiVentas api) implements FormaDeCobro {

    @Override
    public String boton() {
        return "Registrar cobro";
    }

    @Override
    public String explicacion() {
        return "Se cobra en la caja del cine. El efectivo no lleva código de autorización.";
    }

    @Override
    public void enviar(Reserva reserva, MedioPago medio, CajaDeCobro caja) {
        // Un cobro registrado no se deshace: se pregunta antes.
        if (!caja.confirmar("¿Registrar el cobro de la reserva #" + reserva.id() + " en "
                + etiqueta(medio.nombre()).toLowerCase() + "? No se puede deshacer.", "Sí, cobrar")) return;
        caja.pedir(() -> api.cobrar(reserva.id(), medio.nombre(), ""), caja::cobrado);
    }
}
