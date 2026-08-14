package ar.uade.cine.dto.grilla;

import java.util.List;

/** Lo que devuelve el planificador: a quiénes eligió, qué hace con ellos y qué tal salió. */
public record PropuestaGrillaDTO(List<PeliculaElegidaDTO> elenco, List<PaseSugeridoDTO> pases,
                                 IndicadoresGrillaDTO indicadores, int funcionesCreadas) {
}
