package ar.uade.cine.dto.programaciones;

import java.util.List;

// La respuesta de la grilla automática (POST /api/grilla y /propuesta); funcionesCreadas es 0 al proponer.
public record PropuestaGrillaDTO(List<PeliculaElegidaDTO> elenco, List<PaseSugeridoDTO> pases,
                                 IndicadoresGrillaDTO indicadores, int funcionesCreadas) {
}
