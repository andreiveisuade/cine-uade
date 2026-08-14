package ar.uade.cine.dto.usuarios;

/** El login del encargado (CU-10). El cliente no pasa por acá: compra sin registrarse. */
public record PedidoSesionDTO(String email, String password) {
}
