package ar.uade.cine.swing.pantallas.ventas;

import ar.uade.cine.swing.api.ApiVentas;
import ar.uade.cine.swing.api.dto.catalogos.MedioPago;
import ar.uade.cine.swing.api.dto.ventas.Reserva;

// Cobro electrónico: se abre un checkout en la pasarela y el cliente paga ahí.
// Patrón Strategy: ConcreteStrategy de FormaDeCobro para los medios que requieren autorización.
record CobroPorCheckout(ApiVentas api) implements FormaDeCobro {

    @Override
    public String boton() {
        return "Abrir checkout";
    }

    @Override
    public String explicacion() {
        return "El cliente paga en la pasarela y el código de autorización lo devuelve ella.";
    }

    @Override
    public void enviar(Reserva reserva, MedioPago medio, CajaDeCobro caja) {
        // Abrir el checkout no cobra, no se pregunta; valida R5, R17 y R19 antes: si no, hay plata que devolver.
        caja.pedir(() -> api.abrirCheckout(reserva.id(), medio.nombre()), caja::mostrarCheckout);
    }
}
