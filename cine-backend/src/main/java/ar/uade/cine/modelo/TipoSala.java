package ar.uade.cine.modelo;

/**
 * Formato de proyección de la sala. El recargo es sobre el precio base de la función:
 * una 3D cuesta más que una 2D aunque den la misma película.
 */
public enum TipoSala {

    DOS_D(1.0),
    TRES_D(1.3),
    IMAX(1.6),
    CUATRO_D(1.8),
    VIP(2.0);

    private final double multiplicadorPrecio;

    TipoSala(double multiplicadorPrecio) {
        this.multiplicadorPrecio = multiplicadorPrecio;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }
}
