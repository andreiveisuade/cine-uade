package ar.uade.cine.swing.api.dto.promociones;

import java.util.List;

// Una promoción como la lista Swing; el beneficio ajeno a su tipo llega null.
public record Promocion(int id, String nombre, String tipo, Double porcentaje, Double monto, Integer lleva,
                        Integer paga, String vigenciaDesde, String vigenciaHasta, List<String> diasSemana,
                        String horaDesde, String horaHasta, List<String> mediosPago, boolean activa) {
}
