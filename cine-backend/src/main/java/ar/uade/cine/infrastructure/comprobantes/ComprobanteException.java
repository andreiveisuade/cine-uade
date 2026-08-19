package ar.uade.cine.infrastructure.comprobantes;

/**
 * No se pudo escribir un comprobante: el disco lleno, el directorio sin permisos.
 *
 * <p>Reemplaza a la {@code PersistenciaException} que usaban estos generadores cuando la
 * capa de datos era propia. Con Spring Data, los errores de la base ya vienen traducidos a
 * {@code DataAccessException} y esa jerarquía es de la base —no de un archivo de texto en
 * disco—, así que colgar de ahí un ticket que no se pudo imprimir sería mentir sobre qué
 * falló. Las dos terminan igual de todos modos: 500 con mensaje genérico y el detalle al log.
 */
public class ComprobanteException extends RuntimeException {

    public ComprobanteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
