package ar.uade.cine.model.salas;

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
