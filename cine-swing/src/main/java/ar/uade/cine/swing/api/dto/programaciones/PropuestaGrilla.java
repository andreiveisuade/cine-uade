package ar.uade.cine.swing.api.dto.programaciones;

import java.util.List;

// Lo que devuelve el planificador: elenco, pases e indicadores; funcionesCreadas es 0 al solo proponer.
public record PropuestaGrilla(List<PeliculaElegida> elenco, List<PaseSugerido> pases, IndicadoresGrilla indicadores,
                              int funcionesCreadas) {
}
