package ar.uade.cine.swing.api.dto.informes;

import java.util.Map;

public record PeliculaDeclarada(String titulo, String clasificacion, int funciones, int espectadores,
                                Map<String, Integer> entradasPorTarifa, double recaudacionBruta, double descuentos,
                                double recaudacionNeta) {
}
