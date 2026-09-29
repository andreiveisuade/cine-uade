package ar.uade.cine.service;

// Lo pedido por id o código no existe; ManejadorErrores lo contesta con 404 en vez de 400.
// Hereda de IllegalArgumentException para que quien ya atrapaba el rechazo (el importador, los tests) lo siga atrapando.
public class RecursoNoEncontrado extends IllegalArgumentException {

    public RecursoNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
