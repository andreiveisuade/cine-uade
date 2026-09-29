package ar.uade.cine.swing.api.dto.programaciones;

// `motivo` solo si `choca`.
public record FuncionPlanificada(String inicio, boolean choca, String motivo) {
}
