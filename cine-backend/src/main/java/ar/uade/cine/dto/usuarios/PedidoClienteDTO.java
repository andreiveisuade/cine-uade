package ar.uade.cine.dto.usuarios;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al registrar un cliente (POST /api/clientes); exige nombre y email, que valida Usuario.
// Sin @Email: aceptaba «a@b», y reservar da de alta sin pasar por acá. Queda una sola regla, la de Usuario.
public record PedidoClienteDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                               @NotBlank(message = "Falta el email") String email) {
}
