package ar.uade.cine.swing.api.dto.informes;

import ar.uade.cine.swing.api.dto.ventas.Pago;

import java.util.List;
import java.util.Map;

public record Arqueo(String fecha, double total, int entradas, Map<String, Total> porMedio, List<Pago> pagos) {
}
