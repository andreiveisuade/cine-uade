package ar.uade.cine.service;

// Hereda de IllegalArgumentException para que quien ya atrapaba el rechazo (el importador, los tests) lo siga atrapando.
public class RecursoNoEncontrado extends IllegalArgumentException {

    public RecursoNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
