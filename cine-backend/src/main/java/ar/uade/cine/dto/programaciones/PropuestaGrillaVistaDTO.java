package ar.uade.cine.dto.programaciones;

import java.util.List;

// La respuesta de la grilla automática (POST /api/grilla y /propuesta); funcionesCreadas es 0 al proponer.
public record PropuestaGrillaVistaDTO(List<PeliculaElegidaVistaDTO> elenco,
                                      List<PaseSugeridoVistaDTO> pases,
                                      IndicadoresGrillaVistaDTO indicadores, int funcionesCreadas) {
}
