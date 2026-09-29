package ar.uade.cine.swing.api.dto.ventas;

import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;

// Un pago de una reserva, al cobrarla o en el arqueo; película y cliente solo vienen en el arqueo.
public record Pago(int reservaId, double subtotal, double descuento, double monto, String medio, String fecha,
                   String codigoAutorizacion, Pelicula pelicula, Cliente cliente) {
}
