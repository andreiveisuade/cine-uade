package ar.uade.cine.dto.informes;

import java.util.Map;

public record FuncionDeclaradaVistaDTO(int funcionId, String inicio, String sala, String pelicula,
                                       String clasificacion, String idioma, String proyeccion,
                                       int espectadores, Map<String, TotalTarifaDTO> porTarifa,
                                       double recaudacionBruta, double descuentos,
                                       double recaudacionNeta) {
}
