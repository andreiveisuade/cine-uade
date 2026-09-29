package ar.uade.cine.swing.api.dto.cartelera;

// Una corrida del importador de TMDB como la lista Swing: cuántas trajo, salteó y fallaron.
public record Importacion(int id, String estado, String pedidaEn, int nuevas,
                          int salteadas, int fallidas, String detalle) {
}
