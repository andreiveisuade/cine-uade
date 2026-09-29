package ar.uade.cine.service.informes;

import java.util.List;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.dinero.Dinero;

// Borderó de una función más su candy; Experto en sumar el candy, con el total calculado y no guardado.
public record InformeFuncion(Bordero bordero, int comprasCandy, Dinero candy) {

    public static InformeFuncion de(Bordero bordero, List<CompraCandy> compras) {
        return new InformeFuncion(bordero, compras.size(),
                Dinero.sumar(compras.stream().map(CompraCandy::getTotal).toList()));
    }

    public Dinero total() {
        return bordero.recaudacionNeta().mas(candy);
    }
}
