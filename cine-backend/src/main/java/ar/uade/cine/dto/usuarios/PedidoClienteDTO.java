package ar.uade.cine.dto.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Lo que entra al registrar un cliente (POST /api/clientes); Bean Validation exige nombre y email válido.
public record PedidoClienteDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                               @NotBlank(message = "El email no es válido")
                               @Email(message = "El email no es válido") String email) {
}
