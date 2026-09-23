package ar.uade.cine.swing.api.dto;

import java.util.List;

public record Reserva(int id, String estado, String codigo, String ingresadaEn, List<Entrada> entradas,
                      Funcion funcion, Pelicula pelicula, Sala sala, Cliente cliente) {
}
