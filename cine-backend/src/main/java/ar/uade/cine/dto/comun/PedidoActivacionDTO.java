package ar.uade.cine.dto.comun;

import jakarta.validation.constraints.NotNull;

// Lo que entra al dar de baja o reactivar una promoción o una programación (PATCH); exige activa.
public record PedidoActivacionDTO(
        @NotNull(message = "Falta decir si queda activa") Boolean activa) {
}
