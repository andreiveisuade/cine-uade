package ar.uade.cine.dto.programaciones;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code funciones} solo viaja en el detalle: el listado no arrastra lo que generó. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProgramacionVistaDTO(int id, int peliculaId, int salaId, String desde, String hasta,
                                String generadaHasta, String horaInicio, List<String> diasSemana,
                                String idioma, String proyeccion, double precio, boolean activa,
                                List<FuncionGeneradaVistaDTO> funciones) {
}
