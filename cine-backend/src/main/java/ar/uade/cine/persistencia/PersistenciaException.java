package ar.uade.cine.persistencia;

/**
 * Traduce las fallas de cada tecnología (SQLException, IOException) a una excepción
 * propia, para que la capa de negocio no tenga que conocer java.sql.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
