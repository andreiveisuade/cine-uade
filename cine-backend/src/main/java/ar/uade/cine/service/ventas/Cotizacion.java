package ar.uade.cine.service.ventas;

import ar.uade.cine.model.dinero.Dinero;
import ar.uade.cine.model.ventas.MedioPago;
import ar.uade.cine.service.promociones.PoliticaPromociones;

// Lo que se pagaría con un medio: entradas con su promo y el candy pendiente (R15, R16, R30); Experto en el total.
// El checkout pide este mismo total: la cotización no puede prometer un monto distinto del que se cobra.
public record Cotizacion(MedioPago medio, Dinero entradas, PoliticaPromociones.Descuento descuento, Dinero candy) {

    public Dinero entradasConDescuento() {
        return entradas.menos(descuento.monto());
    }

    public Dinero total() {
        return entradasConDescuento().mas(candy);
    }
}
