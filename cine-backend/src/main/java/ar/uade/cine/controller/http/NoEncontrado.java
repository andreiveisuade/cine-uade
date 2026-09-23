package ar.uade.cine.controller.http;

/** El recurso de la URL no existe: 404, a diferencia del 400 de un IllegalArgumentException. */
public class NoEncontrado extends RuntimeException {

    public NoEncontrado(String mensaje) {
        super(mensaje);
    }
}
