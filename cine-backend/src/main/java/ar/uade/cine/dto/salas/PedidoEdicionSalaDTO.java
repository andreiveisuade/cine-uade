package ar.uade.cine.dto.salas;

import jakarta.validation.constraints.NotBlank;

public record PedidoEdicionSalaDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                                   @NotBlank(message = "Falta el tipo de sala") String tipo,
                                   Integer minutosLimpieza) {
}
