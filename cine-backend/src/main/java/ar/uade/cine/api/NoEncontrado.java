package ar.uade.cine.api;

/**
 * El recurso que pide la URL no existe. Se traduce a 404, a diferencia de un dato
 * inválido, que es 400 y lo señalan los gestores con IllegalArgumentException.
 */
public class NoEncontrado extends RuntimeException {

    public NoEncontrado(String mensaje) {
        super(mensaje);
    }
}
