package ar.uade.cine.dto.grilla;

import java.util.List;

public record PropuestaGrillaDTO(List<PeliculaElegidaDTO> elenco, List<PaseSugeridoDTO> pases,
                                 IndicadoresGrillaDTO indicadores, int funcionesCreadas) {
}
