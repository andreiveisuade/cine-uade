package ar.uade.cine.dto.grilla;

import jakarta.validation.constraints.NotNull;

public record PedidoGrillaDTO(String desde, Integer dias, String apertura, String cierre,
                              Integer cuantasPeliculas,
                              @NotNull(message = "Falta el precio de las funciones") Double precio,
                              String idioma, String proyeccion) {
}
