package ar.uade.cine.swing.pantallas.funciones;

import ar.uade.cine.swing.api.dto.funciones.Funcion;

import java.util.function.Function;
import java.util.function.Predicate;

// Una columna de la Agenda, un día o una sala: qué funciones toma y qué dice debajo del título de cada una.
record ColumnaAgenda(String titulo, String detalle, Predicate<Funcion> toma, Function<Funcion, String> subtitulo) {
}
