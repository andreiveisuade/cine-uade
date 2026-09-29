package ar.uade.cine.dto.ventas;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

// Lo que entra al reservar (POST /api/reservas); butacas va código → tarifa, y una tarifa que falta es GENERAL.
// La tarifa viaja como texto, igual que los demás enums: la traduce el controller con Parseo, así acepta
// minúsculas y el error nombra la butaca en vez de «butacas.A1».
public record PedidoReservaDTO(@NotNull(message = "Falta la función") Integer funcionId,
                               String nombre, String email,
                               Map<String, String> butacas,
                               String sesion) {
}
