package ar.uade.cine.dto.cartelera;

import java.util.List;

// Una corrida del importador de TMDB (POST y GET /api/importaciones); estado va por nombre de constante.
public record ImportacionVistaDTO(int id, String estado, int paginas, String pedidaEn,
                                  String terminoEn, int nuevas, int salteadas, int fallidas,
                                  String detalle, List<PeliculaDeCorridaVistaDTO> peliculas) {
}
