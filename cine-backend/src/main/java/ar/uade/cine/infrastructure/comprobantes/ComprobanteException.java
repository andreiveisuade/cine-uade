package ar.uade.cine.infrastructure.comprobantes;

/**
 * No se pudo escribir un comprobante (disco lleno, directorio sin permisos). No cuelga de
 * {@code DataAccessException} porque no falló la base; igual termina en 500 genérico.
 */
public class ComprobanteException extends RuntimeException {

    public ComprobanteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
