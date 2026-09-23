package ar.uade.cine.dto.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PedidoClienteDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                               @NotBlank(message = "El email no es válido")
                               @Email(message = "El email no es válido") String email) {
}
