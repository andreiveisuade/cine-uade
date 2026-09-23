package ar.uade.cine.model.promociones;

/** Discriminador de la tabla única de promociones. */
public enum TipoPromocion {

    /** "Miércoles 30% off". */
    PORCENTAJE,

    /** "$2000 off pagando con Galicia". */
    MONTO_FIJO,

    /** 2x1. */
    NXM
}
