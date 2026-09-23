package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;

public interface GeneradorTicket {

    void emitir(Reserva reserva, Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente);
}
