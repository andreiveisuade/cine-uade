package ar.uade.cine.swing.api.dto.funciones;

import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.salas.Sala;

// Una función como la lee Swing, con su sala y su película; el precio ya viene calculado del backend.
// `libres` solo viene en el detalle (`GET /api/funciones/{id}`); en el listado llega null.
public record Funcion(int id, int peliculaId, int salaId, String inicio, String idioma, String proyeccion,
                      double precio, Sala sala, Pelicula pelicula, Integer libres) {
}
