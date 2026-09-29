package ar.uade.cine.swing.api.dto.informes;

import ar.uade.cine.swing.api.dto.ventas.Pago;

import java.util.List;
import java.util.Map;

// La caja de boletería de un día como la lee Swing: el total, lo cobrado por medio y cada pago.
public record Arqueo(String fecha, double total, int entradas, Map<String, Total> porMedio, List<Pago> pagos) {
}
