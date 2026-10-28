package ar.uade.cine.dto.ventas;

import com.fasterxml.jackson.annotation.JsonInclude;

// Lo que se pagaría con un medio (GET /api/reservas/codigo/{codigo}/cotizacion); sin promo, sin promocion.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CotizacionVistaDTO(String medio, double entradas, double descuento, Integer promocionId,
                                 String promocion, double candy, double total) {
}
