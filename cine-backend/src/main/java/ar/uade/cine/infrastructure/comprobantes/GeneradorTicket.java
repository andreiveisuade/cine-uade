package ar.uade.cine.infrastructure.comprobantes;

import ar.uade.cine.model.cartelera.Pelicula;
import ar.uade.cine.model.funciones.Funcion;
import ar.uade.cine.model.salas.Sala;
import ar.uade.cine.model.usuarios.Cliente;
import ar.uade.cine.model.ventas.Reserva;

/**
 * Emite el comprobante de una reserva. No es un DAO: el ticket se escribe una vez
 * y no se vuelve a leer desde el programa, así que no tiene buscar ni eliminar.
 */
public interface GeneradorTicket {

    void emitir(Reserva reserva, Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente);
}
