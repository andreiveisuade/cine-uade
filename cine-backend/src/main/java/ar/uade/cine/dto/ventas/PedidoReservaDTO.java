package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import ar.uade.cine.model.ventas.TipoTarifa;

public record PedidoReservaDTO(@NotNull(message = "Falta la función") Integer funcionId,
                               String nombre, String email,
                               List<String> codigos, Map<String, TipoTarifa> butacas,
                               String sesion) {
}
