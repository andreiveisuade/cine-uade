package ar.uade.cine.dto.cartelera;

// Una película de una corrida del importador: qué le pasó y, si entró, cómo sigue su revisión hoy.
public record PeliculaDeCorridaVistaDTO(String resultado, Integer peliculaId, String titulo, String motivo,
                                        String estadoRevision) {
}
