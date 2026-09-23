package ar.uade.cine.service.ventas;

public class ButacaOcupadaException extends RuntimeException {

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
