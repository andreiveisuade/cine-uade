package ar.uade.cine.swing.api.dto.funciones;

import java.util.List;

// La respuesta del borrado en lote: las que se borraron y las que no, con el error de cada una tal cual.
public record Borrado(List<Integer> borradas, List<FuncionRechazada> rechazadas) {
}
