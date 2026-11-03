package ar.uade.cine.swing.api.dto.funciones;

import java.util.List;

// Lo que Swing manda al borrar varias funciones de una vez; el tope y R12 los valida el backend.
public record PedidoBorrado(List<Integer> ids) {
}
