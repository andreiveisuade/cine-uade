package ar.uade.cine.dto.usuarios;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al registrar un cliente (POST /api/clientes); exige nombre y email, que valida el modelo.
// Sin @Email: aceptaba «a@b», y reservar da de alta sin pasar por acá. Queda una sola regla, la de Email.
public record PedidoClienteDTO(@NotBlank(message = "Falta el nombre") String nombre,
                               @NotBlank(message = "Falta el email") String email) {
}
