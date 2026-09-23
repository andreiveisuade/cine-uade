package ar.uade.cine.dto.ventas;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

import ar.uade.cine.model.ventas.TipoTarifa;

public record PedidoReservaDTO(@NotNull(message = "Falta la función") Integer funcionId,
                               String nombre, String email,
                               Map<String, TipoTarifa> butacas,
                               String sesion) {
}
