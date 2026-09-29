package ar.uade.cine.dto.informes;

import java.util.List;
import java.util.Map;

import ar.uade.cine.dto.ventas.PagoVistaDTO;

// La caja de boletería de un día (GET /api/arqueo); la arma VistasInformes y cada pago dice qué y a quién.
public record ArqueoVistaDTO(String fecha, double total, int entradas,
                             Map<String, TotalMedioVistaDTO> porMedio, List<PagoVistaDTO> pagos) {
}
