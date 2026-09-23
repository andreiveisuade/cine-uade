package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Reserva(int id, int funcionId, int clienteId, String estado, String creadaEn, String codigo,
                      String ingresadaEn, List<Entrada> entradas, int cantidadEntradas, double total,
                      Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente, Pago pago) {
}
