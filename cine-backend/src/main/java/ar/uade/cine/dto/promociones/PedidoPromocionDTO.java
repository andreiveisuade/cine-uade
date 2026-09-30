package ar.uade.cine.dto.promociones;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

// Lo que entra al cargar una promoción (POST /api/promociones); exige nombre, tipo y vigencia.
// Lo ajeno al tipo va null y se ignora; lo propio lo exige la subclase que crea el tipo.
public record PedidoPromocionDTO(@NotBlank(message = "Falta el nombre") String nombre,
                                 @NotBlank(message = "Falta el tipo de promoción") String tipo,
                                 Double porcentaje, Double monto,
                                 Integer lleva, Integer paga,
                                 @NotBlank(message = "Falta el inicio de la vigencia") String vigenciaDesde,
                                 @NotBlank(message = "Falta el fin de la vigencia") String vigenciaHasta,
                                 List<String> diasSemana, String horaDesde, String horaHasta,
                                 List<String> mediosPago) {
}
