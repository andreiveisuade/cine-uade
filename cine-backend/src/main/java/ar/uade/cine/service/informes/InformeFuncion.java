package ar.uade.cine.service.informes;

import java.util.List;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.dinero.Dinero;

// Borderó de una función más su candy; Experto en sumar el candy, con el total calculado y no guardado.
public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy) {

    // R30: el pedido de la web sin cobrar o anulado no es venta.
    public static InformeFuncion de(Bordero bordero, List<CompraCandy> compras) {
        List<CompraCandy> cobradas = compras.stream().filter(CompraCandy::estaCobrada).toList();
        return new InformeFuncion(bordero, cobradas.size(),
                Dinero.sumar(cobradas.stream().map(CompraCandy::getTotal).toList()));
    }

    public Dinero total() {
        return bordero.recaudacionNeta().mas(candy);
    }
}
