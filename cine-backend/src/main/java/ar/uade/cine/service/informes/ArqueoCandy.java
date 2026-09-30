package ar.uade.cine.service.informes;

import java.time.LocalDate;
import java.util.List;

import ar.uade.cine.model.candy.CompraCandy;
import ar.uade.cine.model.dinero.Dinero;

// Corte de caja del candy de un día: cada compra y su total; Experto en sumarlas, como Arqueo en boletería.
// Total y compras salen de la misma lectura: antes eran dos consultas en transacciones distintas.
public record ArqueoCandy(LocalDate fecha, Dinero total, List<CompraCandy> compras) {

    public static ArqueoCandy de(LocalDate fecha, List<CompraCandy> compras) {
        return new ArqueoCandy(fecha, Dinero.sumar(compras.stream().map(CompraCandy::getTotal).toList()), compras);
    }
}
