package ar.uade.cine.swing.api.dto.cartelera;

// Una película de una corrida: qué le pasó y, si entró, cómo sigue su revisión hoy.
public record PeliculaDeCorrida(String resultado, Integer peliculaId, String titulo, String motivo,
                                String estadoRevision) {
}
