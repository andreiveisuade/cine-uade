package ar.uade.cine.infrastructure.comprobantes;

// Falla de E/S al emitir un ticket o recibo, envuelta en unchecked; ManejadorErrores la traduce a 500.
public class ComprobanteException extends RuntimeException {

    public ComprobanteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
