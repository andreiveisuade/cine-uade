package ar.uade.cine.dto.ventas;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;

/**
 * Un cobro. {@code pelicula}, {@code cliente} y {@code entradas} solo viajan en el arqueo,
 * que muestra qué se cobró y no solo cuánto.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoVistaDTO(int id, int reservaId, double subtotal, Integer promocionId,
                        double descuento, double monto, String medio, String fecha,
                        String codigoAutorizacion, PeliculaVistaDTO pelicula, ClienteVistaDTO cliente,
                        Integer entradas) {
}
