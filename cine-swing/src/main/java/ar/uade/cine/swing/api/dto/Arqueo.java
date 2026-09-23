package ar.uade.cine.swing.api.dto;

import java.util.List;
import java.util.Map;

public record Arqueo(String fecha, double total, int entradas, Map<String, Total> porMedio, List<Pago> pagos) {
}
