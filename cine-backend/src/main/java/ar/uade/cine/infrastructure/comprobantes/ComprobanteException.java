package ar.uade.cine.infrastructure.comprobantes;

public class ComprobanteException extends RuntimeException {

    public ComprobanteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
