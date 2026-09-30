package ar.uade.cine.dto.programaciones;

import jakarta.validation.constraints.NotNull;

// Lo que entra a la grilla automática (POST /api/grilla y /propuesta); solo el precio no tiene default.
public record PedidoGrillaDTO(String desde, Integer dias, String apertura, String cierre,
                              Integer cuantasPeliculas,
                              @NotNull(message = "Falta el precio") Double precio,
                              String idioma, String proyeccion) {
}
