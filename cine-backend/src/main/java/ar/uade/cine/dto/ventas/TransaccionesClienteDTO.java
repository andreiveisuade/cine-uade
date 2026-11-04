package ar.uade.cine.dto.ventas;

import java.util.List;

import ar.uade.cine.dto.candy.CompraCandyVistaDTO;

// Lo que compró un cliente: sus reservas, con su pago y candy, y el candy que no va con una reserva.
public record TransaccionesClienteDTO(List<ReservaVistaDTO> reservas, List<CompraCandyVistaDTO> comprasCandy) {
}
