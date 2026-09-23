package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

import ar.uade.cine.model.ventas.TipoTarifa;

public record PedidoReservaDTO(Integer funcionId, String nombre, String email,
                            List<String> codigos, Map<String, TipoTarifa> butacas,
                            String sesion) {
}
