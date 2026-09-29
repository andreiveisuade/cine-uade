package ar.uade.cine.dto.informes;

import java.util.Map;

// Una función de la declaración jurada con las cifras de su borderó; idioma y proyección por constante.
public record FuncionDeclaradaVistaDTO(int funcionId, String inicio, String sala, String pelicula,
                                       String clasificacion, String idioma, String proyeccion,
                                       int espectadores, Map<String, TotalTarifaVistaDTO> porTarifa,
                                       double recaudacionBruta, double descuentos,
                                       double recaudacionNeta) {
}
