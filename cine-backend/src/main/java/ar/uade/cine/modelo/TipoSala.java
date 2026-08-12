package ar.uade.cine.modelo;

/**
 * Formato de proyección que soporta la sala. El multiplicador es sobre el precio base
 * de la función: una butaca de IMAX cuesta más que una de 2D aunque den la misma película.
 */
public enum TipoSala {

    DOS_D(1.0, false),
    TRES_D(1.3, true),
    IMAX(1.6, true),
    CUATRO_D(1.8, true),
    VIP(2.0, true);

    private final double multiplicadorPrecio;
    private final boolean soportaTresD;

    TipoSala(double multiplicadorPrecio, boolean soportaTresD) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.soportaTresD = soportaTresD;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }

    /** Una sala 2D no tiene el proyector para dar funciones en 3D. */
    public boolean soportaTresD() {
        return soportaTresD;
    }
}
