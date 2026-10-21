package ar.uade.cine.swing.api.dto.candy;

import com.fasterxml.jackson.annotation.JsonInclude;

// Lo que Swing manda al editar un producto o combo. El PUT reemplaza todo: imagenUrl en null es "sin imagen".
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PedidoEdicionProducto(String nombre, Double precio, String imagenUrl) {
}
