package ar.uade.cine.dto.comun;

// El error de cualquier rechazo, {"error": "..."}; lo arma ManejadorErrores y el texto llega tal cual.
public record ErrorVistaDTO(String error) {
}
