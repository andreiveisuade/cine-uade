package ar.uade.cine.dominio.promociones;

/**
 * Qué clase de beneficio da la promoción. Es el discriminador de la tabla única, igual
 * que {@code Rol} en usuario: las tres van a la misma tabla porque comparten todas las
 * condiciones y solo cambia cómo calculan el descuento.
 */
public enum TipoPromocion {

    /** Un porcentaje del subtotal. "Miércoles 30% off". */
    PORCENTAJE,

    /** Una cantidad de plata fija. "$2000 off pagando con Galicia". */
    MONTO_FIJO,

    /**
     * Llevás n y pagás m. El clásico 2x1, que es la razón por la que el descuento se
     * calcula sobre el conjunto de entradas y no butaca por butaca.
     */
    NXM
}
