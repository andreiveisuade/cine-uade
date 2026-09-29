package ar.uade.cine.swing.api.dto.ventas;

import ar.uade.cine.swing.api.dto.cartelera.Pelicula;
import ar.uade.cine.swing.api.dto.funciones.Funcion;
import ar.uade.cine.swing.api.dto.salas.Sala;
import ar.uade.cine.swing.api.dto.usuarios.Cliente;

import java.util.List;

public record Reserva(int id, String estado, String codigo, String ingresadaEn, List<Entrada> entradas, double total,
                      Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente, Pago pago,
                      boolean cobrable, boolean cancelable) {
}
