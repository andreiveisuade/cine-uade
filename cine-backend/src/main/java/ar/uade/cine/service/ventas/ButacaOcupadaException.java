package ar.uade.cine.service.ventas;

// Butaca ya tomada por otro (R4), al elegir o en la carrera del UNIQUE; ManejadorErrores la contesta con 409.
// Un solo status para los dos momentos: la web vuelve al mapa recargado solo ante un 409.
public class ButacaOcupadaException extends RuntimeException {

    public ButacaOcupadaException(String mensaje) {
        super(mensaje);
    }

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
