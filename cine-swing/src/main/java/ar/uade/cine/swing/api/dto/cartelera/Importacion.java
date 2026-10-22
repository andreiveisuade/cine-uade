package ar.uade.cine.swing.api.dto.cartelera;

import java.util.List;

// Una corrida del importador de TMDB como la lista Swing: cuántas trajo, salteó y fallaron, y cuáles.
public record Importacion(int id, String estado, int paginas, String pedidaEn, int nuevas,
                          int salteadas, int fallidas, String detalle, List<PeliculaDeCorrida> peliculas) {
}
