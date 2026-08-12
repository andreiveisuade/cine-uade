package ar.uade.cine.modelo;

/**
 * No todas las butacas de una sala son iguales: las accesibles van al final de fila,
 * las de pareja no tienen apoyabrazos en el medio y las VIP son más caras.
 */
public enum TipoAsiento {

    ESTANDAR(1.0),
    VIP(1.5),
    PAREJA(1.8),
    ACCESIBLE(1.0);

    private final double multiplicadorPrecio;

    TipoAsiento(double multiplicadorPrecio) {
        this.multiplicadorPrecio = multiplicadorPrecio;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }
}
