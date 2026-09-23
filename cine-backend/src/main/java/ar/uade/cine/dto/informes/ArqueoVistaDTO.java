package ar.uade.cine.dto.informes;

import java.util.List;
import java.util.Map;

import ar.uade.cine.dto.ventas.PagoVistaDTO;

public record ArqueoVistaDTO(String fecha, double total, int entradas, Map<String, TotalMedioDTO> porMedio,
                          List<PagoVistaDTO> pagos) {
}
