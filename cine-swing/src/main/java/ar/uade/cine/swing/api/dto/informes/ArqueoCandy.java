package ar.uade.cine.swing.api.dto.informes;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;

import java.util.List;

// La caja del candy de un día, aparte de la boletería: el total y cada venta, de mostrador o no.
public record ArqueoCandy(String fecha, double total, List<CompraCandy> compras) {
}
