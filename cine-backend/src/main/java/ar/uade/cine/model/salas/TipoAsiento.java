package ar.uade.cine.model.salas;

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
