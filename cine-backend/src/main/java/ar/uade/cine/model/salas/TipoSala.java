package ar.uade.cine.model.salas;

/**
 * Tecnología de la sala; su multiplicador va sobre el precio base. Lo premium de la butaca
 * lo dice {@link TipoAsiento}: en los dos lados, el recargo se cobraría dos veces.
 */
public enum TipoSala {

    DOS_D(1.0, false),
    TRES_D(1.3, true),
    IMAX(1.6, true),
    CUATRO_D(1.8, true);

    private final double multiplicadorPrecio;
    private final boolean soportaTresD;

    TipoSala(double multiplicadorPrecio, boolean soportaTresD) {
        this.multiplicadorPrecio = multiplicadorPrecio;
        this.soportaTresD = soportaTresD;
    }

    public double getMultiplicadorPrecio() {
        return multiplicadorPrecio;
    }

    public boolean soportaTresD() {
        return soportaTresD;
    }
}
