package ar.uade.cine.dto.usuarios;

// Si el login con Google está armado (GET /api/sesion/google), para que la web no mande a una ruta que da 401.
public record IngresoConGoogleVistaDTO(boolean disponible) {
}
