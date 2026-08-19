package ar.uade.cine.dto.funciones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;

/**
 * Una función con todo lo que arrastra y no guarda: la sala donde va, la película que
 * proyecta y —solo en el detalle— el mapa de butacas con cuáles están tomadas.
 *
 * <p>Los campos que no aplican viajan en null y Jackson los deja afuera: el listado del
 * cliente no necesita las butacas, y mandarlas en vacío haría creer que la sala no tiene.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionVistaDTO(int id, int peliculaId, int salaId, String inicio, String idioma,
                           String proyeccion, double precio, double precioDesde, SalaVistaDTO sala,
                           PeliculaVistaDTO pelicula, List<AsientoVistaDTO> asientos, Integer libres) {
}
