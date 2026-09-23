package ar.uade.cine.dto.programaciones;

import java.util.List;

public record PedidoProgramacionDTO(Integer peliculaId, Integer salaId, String desde, String hasta,
                                 String horaInicio, List<String> diasSemana, String idioma,
                                 String proyeccion, Double precio) {
}
