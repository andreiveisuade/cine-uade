package ar.uade.cine.dto.ventas;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.uade.cine.dto.usuarios.ClienteVistaDTO;
import ar.uade.cine.dto.cartelera.PeliculaVistaDTO;

// Un pago de una reserva (/api/reservas/{id}/pago y arqueo); película, cliente y entradas solo en el arqueo.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoVistaDTO(int id, int reservaId, double subtotal, Integer promocionId,
                           double descuento, double monto, String medio, String fecha,
                           String codigoAutorizacion, PeliculaVistaDTO pelicula, ClienteVistaDTO cliente,
                           Integer entradas) {
}
