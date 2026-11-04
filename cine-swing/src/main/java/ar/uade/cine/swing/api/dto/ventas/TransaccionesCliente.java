package ar.uade.cine.swing.api.dto.ventas;

import ar.uade.cine.swing.api.dto.candy.CompraCandy;

import java.util.List;

// Lo que compró un cliente: sus reservas y el candy que no va con una reserva; cada lista, lo más nuevo primero.
public record TransaccionesCliente(List<Reserva> reservas, List<CompraCandy> comprasCandy) {
}
