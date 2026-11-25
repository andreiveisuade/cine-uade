package ar.uade.cine.dto.comentarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que entra al publicar un comentario (POST /api/comentarios); sin autor: sale de la sesión.
public record PedidoComentarioDTO(
        @Schema(example = "5")
        @NotNull(message = "Falta el puntaje") Integer puntaje,
        @Schema(example = "Muy buena imagen y sonido")
        @NotBlank(message = "Falta la reseña") String texto) {
}
