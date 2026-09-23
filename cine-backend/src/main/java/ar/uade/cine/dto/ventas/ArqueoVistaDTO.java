package ar.uade.cine.dto.ventas;

import java.util.List;
import java.util.Map;

/** La clave de {@code porMedio} es el nombre del enum. */
public record ArqueoVistaDTO(String fecha, double total, int entradas, Map<String, TotalMedioDTO> porMedio,
                          List<PagoVistaDTO> pagos) {
}
