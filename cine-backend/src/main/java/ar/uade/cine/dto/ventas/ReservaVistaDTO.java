package ar.uade.cine.dto.ventas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.funciones.FuncionVistaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;
import ar.uade.cine.dto.salas.SalaVistaDTO;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservaVistaDTO(int id, int funcionId, int clienteId, String estado, String creadaEn,
                           String codigo, String ingresadaEn,
                           List<EntradaVistaDTO> entradas, int cantidadEntradas, double total,
                           FuncionVistaDTO funcion, PeliculaVistaDTO pelicula, SalaVistaDTO sala,
                           ClienteVistaDTO cliente, PagoVistaDTO pago) {
}
