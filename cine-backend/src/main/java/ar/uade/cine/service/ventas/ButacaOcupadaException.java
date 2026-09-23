package ar.uade.cine.service.ventas;

/**
 * Otra reserva se quedó con la butaca entre validar y guardar. Es una carrera perdida, no
 * una falla: la API contesta 409 y el front vuelve a pedir el mapa.
 */
public class ButacaOcupadaException extends RuntimeException {

    public ButacaOcupadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
