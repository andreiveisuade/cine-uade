package ar.uade.cine.dto.funciones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dto.salas.AsientoVistaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionVistaDTO(int id, int peliculaId, int salaId, String inicio, String idioma,
                           String proyeccion, double precio, double precioDesde, SalaVistaDTO sala,
                           PeliculaVistaDTO pelicula, List<AsientoVistaDTO> asientos, Integer libres) {
}
