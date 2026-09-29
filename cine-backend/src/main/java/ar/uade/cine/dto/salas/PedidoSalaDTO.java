package ar.uade.cine.dto.salas;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// Lo que entra al dar de alta una sala (POST /api/salas); exige nombre, tipo y filas, no la limpieza.
public record PedidoSalaDTO(@NotBlank(message = "El nombre no puede estar vacío") String nombre,
                            @NotBlank(message = "Falta el tipo de sala") String tipo,
                            @NotEmpty(message = "La sala tiene que tener al menos una fila") List<Integer> butacasPorFila,
                            List<String> codigosVip, List<String> codigosPareja,
                            List<String> codigosAccesibles, Integer minutosLimpieza) {
}
