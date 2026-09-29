package ar.uade.cine.swing.api.dto.informes;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;

import java.util.List;

public record ArqueoCandy(String fecha, double total, List<CompraCandy> compras) {
}
