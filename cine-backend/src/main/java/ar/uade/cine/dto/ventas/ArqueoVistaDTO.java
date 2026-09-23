package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

public record ArqueoVistaDTO(String fecha, double total, int entradas, Map<String, TotalMedioDTO> porMedio,
                          List<PagoVistaDTO> pagos) {
}
