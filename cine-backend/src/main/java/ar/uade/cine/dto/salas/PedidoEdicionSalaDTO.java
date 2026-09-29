package ar.uade.cine.dto.salas;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al editar una sala (PUT /api/salas/{id}); exige nombre y tipo, sin limpieza queda la anterior.
public record PedidoEdicionSalaDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                                   @NotBlank(message = "Falta el tipo de sala") String tipo,
                                   Integer minutosLimpieza) {
}
