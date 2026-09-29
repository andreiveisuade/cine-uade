package ar.uade.cine.service.ventas;

// Se perdió la carrera por el UNIQUE de entrada (R4) al confirmar; ManejadorErrores la contesta con 409.
public class ButacaOcupadaException extends RuntimeException {

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
