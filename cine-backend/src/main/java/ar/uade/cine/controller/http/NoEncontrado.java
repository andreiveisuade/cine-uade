package ar.uade.cine.controller.http;

public class NoEncontrado extends RuntimeException {

    public NoEncontrado(String mensaje) {
        super(mensaje);
    }
}
