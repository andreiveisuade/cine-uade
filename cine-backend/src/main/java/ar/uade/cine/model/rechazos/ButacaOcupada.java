package ar.uade.cine.model.rechazos;

// Butaca ya tomada por otro (R4), al elegir o en la carrera del UNIQUE; ManejadorErrores la contesta con 409.
// Un solo status para los dos momentos: la web vuelve al mapa recargado solo ante un 409.
public final class ButacaOcupada extends Rechazo {

    public ButacaOcupada(String mensaje) {
        super(mensaje);
    }

    public ButacaOcupada(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
