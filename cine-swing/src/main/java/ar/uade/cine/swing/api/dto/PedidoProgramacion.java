package ar.uade.cine.swing.api.dto;

import java.util.List;

// `diasSemana` vacío = todos los días. Es `idioma`, no `version`: así le dice la API.
public record PedidoProgramacion(Integer peliculaId, Integer salaId, String desde, String hasta, String horaInicio,
                                 List<String> diasSemana, String idioma, String proyeccion, Double precio) {
}
