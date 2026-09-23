package ar.uade.cine.swing.api.dto;

import java.util.List;

public record PropuestaGrilla(List<PeliculaElegida> elenco, List<PaseSugerido> pases, IndicadoresGrilla indicadores,
                              int funcionesCreadas) {
}
