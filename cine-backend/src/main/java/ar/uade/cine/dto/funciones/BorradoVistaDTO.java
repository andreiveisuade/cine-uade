package ar.uade.cine.dto.funciones;

import java.util.List;

// Lo que sale de un borrado en lote: las que se borraron y las que no, cada una con el error del borrado de a una.
public record BorradoVistaDTO(List<Integer> borradas, List<FuncionRechazadaVistaDTO> rechazadas) {
}
