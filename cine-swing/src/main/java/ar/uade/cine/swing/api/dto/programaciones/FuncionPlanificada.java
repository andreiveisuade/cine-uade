package ar.uade.cine.swing.api.dto.programaciones;

// Un pase del plan de una grilla: cuándo es y si choca con otra función de la sala.
// `motivo` solo si `choca`.
public record FuncionPlanificada(String inicio, boolean choca, String motivo) {
}
