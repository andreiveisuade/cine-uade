package ar.uade.cine.swing.api.dto.catalogos;

import ar.uade.cine.swing.api.dto.promociones.PedidoPromocion;

import java.util.List;

// Un tipo de promoción del catálogo; Swing arma con él la tarjeta de beneficio del alta.
/** {@code campos}: los de {@link PedidoPromocion} que pide este tipo, con el nombre que tienen en el JSON. */
public record TipoPromocion(String nombre, List<String> campos) {
}
