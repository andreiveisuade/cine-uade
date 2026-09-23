package ar.uade.cine.dto.programaciones;

import java.util.List;

public record PropuestaGrillaDTO(List<PeliculaElegidaDTO> elenco, List<PaseSugeridoDTO> pases,
                                 IndicadoresGrillaDTO indicadores, int funcionesCreadas) {
}
